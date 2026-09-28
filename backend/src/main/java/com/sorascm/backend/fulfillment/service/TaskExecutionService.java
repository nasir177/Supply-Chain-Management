package com.sorascm.backend.fulfillment.service;

import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.fulfillment.dto.TaskExecutionDto;
import com.sorascm.backend.fulfillment.entity.*;
import com.sorascm.backend.fulfillment.repository.PackTaskRepository;
import com.sorascm.backend.fulfillment.repository.PickTaskRepository;
import com.sorascm.backend.fulfillment.repository.SalesOrderRepository;
import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.product.repository.ProductRepository;
import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import com.sorascm.backend.warehouse.repository.WarehouseLocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskExecutionService {

    private final PickTaskRepository pickTaskRepository;
    private final PackTaskRepository packTaskRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository locationRepository;

    public TaskExecutionService(
            PickTaskRepository pickTaskRepository,
            PackTaskRepository packTaskRepository,
            SalesOrderRepository salesOrderRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository locationRepository
    ) {
        this.pickTaskRepository = pickTaskRepository;
        this.packTaskRepository = packTaskRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Generates physical pick instructions for an allocated order.
     */
    @Transactional
    public List<TaskExecutionDto.PickTaskResponse> generatePickTasks(UUID salesOrderId, Long defaultSourceLocationId) {
        SalesOrder so = salesOrderRepository.findByIdWithLock(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", salesOrderId));

        if (so.getStatus() != SalesOrderStatus.ALLOCATED) {
            throw new BusinessException("INVALID_STATUS", "Order must be ALLOCATED to generate pick tasks", HttpStatus.CONFLICT);
        }

        WarehouseLocation location = locationRepository.findById(defaultSourceLocationId)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseLocation", defaultSourceLocationId));

        for (SalesOrderLine line : so.getLines()) {
            PickTask pickTask = new PickTask();
            pickTask.setTaskNumber("PICK-" + System.currentTimeMillis() + "-" + line.getId());
            pickTask.setSalesOrder(so);
            pickTask.setSalesOrderLine(line);
            pickTask.setProduct(line.getProduct());
            pickTask.setSourceLocation(location);
            pickTask.setQuantityToPick(line.getQuantityAllocated());
            pickTask.setStatus(PickTaskStatus.PENDING);
            pickTaskRepository.save(pickTask);
        }

        so.setStatus(SalesOrderStatus.PICKING);
        salesOrderRepository.save(so);

        return getPickTasksForOrder(salesOrderId);
    }

    /**
     * Operator confirms item retrieval from shelf bin.
     */
    @Transactional
    public TaskExecutionDto.PickTaskResponse confirmPick(UUID pickTaskId, TaskExecutionDto.ConfirmPickRequest req) {
        PickTask task = pickTaskRepository.findById(pickTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("PickTask", pickTaskId));

        if (task.getStatus() == PickTaskStatus.COMPLETED) {
            throw new BusinessException("TASK_ALREADY_COMPLETED", "Task is already confirmed", HttpStatus.CONFLICT);
        }

        if (req.quantityPicked() > task.getQuantityToPick()) {
            throw new BusinessException("OVERPICK_NOT_ALLOWED", "Cannot pick more than requested", HttpStatus.BAD_REQUEST);
        }

        task.setQuantityPicked(req.quantityPicked());
        task.setAssignedPicker(req.pickerName());
        task.setStatus(PickTaskStatus.COMPLETED);
        task.setPickedAt(Instant.now());
        PickTask saved = pickTaskRepository.save(task);

        // Check if all lines for the sales order are picked
        List<PickTask> orderPicks = pickTaskRepository.findBySalesOrderId(task.getSalesOrder().getId());
        boolean allFinished = orderPicks.stream().allMatch(p -> p.getStatus() == PickTaskStatus.COMPLETED);
        if (allFinished) {
            // All pick tasks complete; order is fully picked
            task.getSalesOrder().setStatus(SalesOrderStatus.PICKING);
            salesOrderRepository.save(task.getSalesOrder());
        }

        return mapToPickResponse(saved);
    }

    /**
     * Packing station seals items into physical shipment box with measured weight.
     */
    @Transactional
    public TaskExecutionDto.PackTaskResponse createPackTask(UUID salesOrderId, TaskExecutionDto.CreatePackRequest req) {
        SalesOrder so = salesOrderRepository.findByIdWithLock(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", salesOrderId));

        PackTask packTask = new PackTask();
        packTask.setPackNumber("BOX-" + System.currentTimeMillis());
        packTask.setSalesOrder(so);
        packTask.setContainerType(req.containerType());
        packTask.setWeightKg(req.weightKg());
        packTask.setPackerName(req.packerName());
        packTask.setStatus(PackTaskStatus.PACKED);
        packTask.setPackedAt(Instant.now());

        for (TaskExecutionDto.PackLineItem item : req.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));

            PackTaskLine line = new PackTaskLine();
            line.setProduct(product);
            line.setQuantityPacked(item.quantity());
            packTask.addLine(line);
        }

        PackTask saved = packTaskRepository.save(packTask);

        // Transition order status to PACKED
        so.setStatus(SalesOrderStatus.PACKED);
        salesOrderRepository.save(so);

        return mapToPackResponse(saved);
    }

    public List<TaskExecutionDto.PickTaskResponse> getPickTasksForOrder(UUID salesOrderId) {
        return pickTaskRepository.findBySalesOrderId(salesOrderId).stream()
                .map(this::mapToPickResponse)
                .toList();
    }

    public List<TaskExecutionDto.PackTaskResponse> getPackTasksForOrder(UUID salesOrderId) {
        return packTaskRepository.findBySalesOrderId(salesOrderId).stream()
                .map(this::mapToPackResponse)
                .toList();
    }

    private TaskExecutionDto.PickTaskResponse mapToPickResponse(PickTask t) {
        return new TaskExecutionDto.PickTaskResponse(
                t.getId(),
                t.getTaskNumber(),
                t.getSalesOrder().getId(),
                t.getProduct().getId(),
                t.getProduct().getSku(),
                t.getProduct().getName(),
                t.getSourceLocation().getId(),
                t.getSourceLocation().getCode(),
                t.getQuantityToPick(),
                t.getQuantityPicked(),
                t.getStatus(),
                t.getAssignedPicker(),
                t.getPickedAt()
        );
    }

    private TaskExecutionDto.PackTaskResponse mapToPackResponse(PackTask p) {
        return new TaskExecutionDto.PackTaskResponse(
                p.getId(),
                p.getPackNumber(),
                p.getSalesOrder().getId(),
                p.getContainerType(),
                p.getWeightKg(),
                p.getStatus(),
                p.getPackerName(),
                p.getPackedAt(),
                p.getLines().stream().map(l -> new TaskExecutionDto.PackLineResponse(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getQuantityPacked()
                )).toList()
        );
    }
}
package com.sorascm.backend.fulfillment.service;

import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.fulfillment.dto.FulfillmentDto;
import com.sorascm.backend.fulfillment.entity.Customer;
import com.sorascm.backend.fulfillment.entity.SalesOrder;
import com.sorascm.backend.fulfillment.entity.SalesOrderLine;
import com.sorascm.backend.fulfillment.entity.SalesOrderStatus;
import com.sorascm.backend.fulfillment.repository.CustomerRepository;
import com.sorascm.backend.fulfillment.repository.SalesOrderRepository;
import com.sorascm.backend.inventory.service.InventoryService;
import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.product.repository.ProductRepository;
import com.sorascm.backend.warehouse.entity.Warehouse;
import com.sorascm.backend.warehouse.repository.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FulfillmentService {

    private final CustomerRepository customerRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public FulfillmentService(
            CustomerRepository customerRepository,
            SalesOrderRepository salesOrderRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            InventoryService inventoryService
    ) {
        this.customerRepository = customerRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Customer createCustomer(FulfillmentDto.CreateCustomerRequest req) {
        if (customerRepository.existsByCustomerCode(req.customerCode())) {
            throw new BusinessException("DUPLICATE_CUSTOMER", "Customer code already exists: " + req.customerCode(), HttpStatus.CONFLICT);
        }
        Customer c = new Customer();
        c.setCustomerCode(req.customerCode().toUpperCase());
        c.setName(req.name());
        c.setEmail(req.email());
        c.setPhone(req.phone());
        c.setShippingAddressLine1(req.addressLine1());
        c.setShippingCity(req.city());
        c.setShippingCountryCode(req.countryCode().toUpperCase());
        return customerRepository.save(c);
    }

    @Transactional
    public FulfillmentDto.SoResponse createSalesOrder(FulfillmentDto.CreateSoRequest req) {
        Customer customer = customerRepository.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", req.customerId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", req.warehouseId()));

        SalesOrder so = new SalesOrder();
        so.setOrderNumber("SO-" + System.currentTimeMillis());
        so.setCustomer(customer);
        so.setWarehouse(warehouse);
        so.setStatus(SalesOrderStatus.CREATED);
        so.setNotes(req.notes());

        BigDecimal total = BigDecimal.ZERO;
        for (FulfillmentDto.CreateSoLineItem item : req.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));

            SalesOrderLine line = new SalesOrderLine();
            line.setProduct(product);
            line.setQuantityOrdered(item.quantity());
            line.setUnitPrice(item.unitPrice());
            so.addLine(line);

            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        so.setTotalAmount(total);
        SalesOrder saved = salesOrderRepository.save(so);
        return mapToSoResponse(saved);
    }

    @Transactional
    public FulfillmentDto.SoResponse allocateSalesOrder(UUID salesOrderId, Long locationId) {
        SalesOrder so = salesOrderRepository.findByIdWithLock(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", salesOrderId));

        if (so.getStatus() != SalesOrderStatus.CREATED) {
            throw new BusinessException("INVALID_STATUS", "Order must be in CREATED state to allocate", HttpStatus.CONFLICT);
        }

        for (SalesOrderLine line : so.getLines()) {
            int needed = line.getQuantityOrdered() - line.getQuantityAllocated();
            if (needed > 0) {
                inventoryService.allocateStock(line.getProduct().getId(), locationId, needed, so.getId());
                line.setQuantityAllocated(line.getQuantityOrdered());
            }
        }

        so.setStatus(SalesOrderStatus.ALLOCATED);
        return mapToSoResponse(salesOrderRepository.save(so));
    }

    @Transactional
    public FulfillmentDto.SoResponse shipSalesOrder(UUID salesOrderId, FulfillmentDto.ShipRequest req) {
        SalesOrder so = salesOrderRepository.findByIdWithLock(salesOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", salesOrderId));

        if (so.getStatus() != SalesOrderStatus.ALLOCATED) {
            throw new BusinessException("INVALID_STATUS", "Order must be in ALLOCATED state to ship", HttpStatus.CONFLICT);
        }

        for (SalesOrderLine line : so.getLines()) {
            int toShip = line.getQuantityAllocated() - line.getQuantityShipped();
            if (toShip > 0) {
                inventoryService.dispatchStock(line.getProduct().getId(), req.sourceLocationId(), toShip, so.getId());
                line.setQuantityShipped(line.getQuantityOrdered());
            }
        }

        so.setShippingCarrier(req.carrier());
        so.setTrackingNumber(req.trackingNumber());
        so.setStatus(SalesOrderStatus.SHIPPED);

        return mapToSoResponse(salesOrderRepository.save(so));
    }

    private FulfillmentDto.SoResponse mapToSoResponse(SalesOrder so) {
        return new FulfillmentDto.SoResponse(
                so.getId(),
                so.getOrderNumber(),
                so.getCustomer().getId(),
                so.getWarehouse().getId(),
                so.getStatus(),
                so.getTotalAmount(),
                so.getShippingCarrier(),
                so.getTrackingNumber(),
                so.getLines().stream().map(l -> new FulfillmentDto.SoLineResponse(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantityOrdered(),
                        l.getQuantityAllocated(),
                        l.getQuantityShipped(),
                        l.getUnitPrice()
                )).toList()
        );
    }
}
package com.sorascm.backend.logistics.service;

import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.fulfillment.entity.PackTask;
import com.sorascm.backend.fulfillment.entity.SalesOrder;
import com.sorascm.backend.fulfillment.entity.SalesOrderStatus;
import com.sorascm.backend.fulfillment.repository.PackTaskRepository;
import com.sorascm.backend.fulfillment.repository.SalesOrderRepository;
import com.sorascm.backend.logistics.dto.LogisticsDto;
import com.sorascm.backend.logistics.entity.*;
import com.sorascm.backend.logistics.repository.ShipmentRepository;
import com.sorascm.backend.logistics.repository.ShippingManifestRepository;
import com.sorascm.backend.outbox.service.OutboxService;
import com.sorascm.backend.warehouse.entity.Warehouse;
import com.sorascm.backend.warehouse.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LogisticsService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsService.class);

    private final ShippingManifestRepository manifestRepository;
    private final ShipmentRepository shipmentRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final PackTaskRepository packTaskRepository;
    private final OutboxService outboxService;

    public LogisticsService(
            ShippingManifestRepository manifestRepository,
            ShipmentRepository shipmentRepository,
            SalesOrderRepository salesOrderRepository,
            WarehouseRepository warehouseRepository,
            PackTaskRepository packTaskRepository,
            OutboxService outboxService
    ) {
        this.manifestRepository = manifestRepository;
        this.shipmentRepository = shipmentRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.warehouseRepository = warehouseRepository;
        this.packTaskRepository = packTaskRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public LogisticsDto.ManifestResponse createManifest(LogisticsDto.CreateManifestRequest req) {
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", req.warehouseId()));

        ShippingManifest manifest = new ShippingManifest();
        manifest.setManifestNumber("MNF-" + System.currentTimeMillis());
        manifest.setCarrier(req.carrier().toUpperCase());
        manifest.setWarehouse(warehouse);
        manifest.setStatus(ManifestStatus.OPEN);
        manifest.setScheduledPickupAt(req.scheduledPickupAt());

        return mapToManifestResponse(manifestRepository.save(manifest));
    }

    @Transactional
    public LogisticsDto.ShipmentResponse createShipment(LogisticsDto.CreateShipmentRequest req) {
        SalesOrder so = salesOrderRepository.findById(req.salesOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("SalesOrder", req.salesOrderId()));

        if (so.getStatus() != SalesOrderStatus.PACKED) {
            throw new BusinessException("ORDER_NOT_PACKED", "Order must be PACKED before generating shipment documents", HttpStatus.CONFLICT);
        }

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber("SHP-" + System.currentTimeMillis());
        shipment.setSalesOrder(so);
        shipment.setCarrier(req.carrier().toUpperCase());
        shipment.setTrackingNumber(req.trackingNumber());
        if (req.serviceLevel() != null) shipment.setServiceLevel(req.serviceLevel());
        shipment.setStatus(ShipmentStatus.MANIFESTED);
        shipment.setRecipientName(so.getCustomer().getName());
        shipment.setDestinationAddress(so.getCustomer().getShippingAddressLine1());
        shipment.setDestinationCity(so.getCustomer().getShippingCity());
        shipment.setDestinationCountryCode(so.getCustomer().getShippingCountryCode());
        shipment.setEstimatedDeliveryAt(req.estimatedDeliveryAt());

        if (req.manifestId() != null) {
            ShippingManifest manifest = manifestRepository.findById(req.manifestId())
                    .orElseThrow(() -> new ResourceNotFoundException("ShippingManifest", req.manifestId()));
            shipment.setManifest(manifest);
            manifest.setTotalPackages(manifest.getTotalPackages() + 1);

            List<PackTask> packs = packTaskRepository.findBySalesOrderId(so.getId());
            BigDecimal orderWeight = packs.stream()
                    .map(p -> p.getWeightKg() != null ? p.getWeightKg() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            manifest.setTotalWeightKg(manifest.getTotalWeightKg().add(orderWeight));
            manifestRepository.save(manifest);
        }

        ShipmentEvent initialEvent = new ShipmentEvent();
        initialEvent.setFromStatus(null);
        initialEvent.setToStatus(ShipmentStatus.MANIFESTED);
        initialEvent.setLocationDescription(so.getWarehouse().getName());
        initialEvent.setCarrierMessage("Shipping label and manifest document generated");
        initialEvent.setEventTimestamp(Instant.now());
        shipment.addEvent(initialEvent);

        Shipment saved = shipmentRepository.save(shipment);
        so.setShippingCarrier(saved.getCarrier());
        so.setTrackingNumber(saved.getTrackingNumber());
        salesOrderRepository.save(so);

        return mapToShipmentResponse(saved);
    }

    /**
     * Ingests external carrier tracking status updates and validates state transitions.
     */
    @Transactional
    public LogisticsDto.ShipmentResponse processCarrierWebhook(LogisticsDto.TrackingWebhookEvent event) {
        Shipment shipment = shipmentRepository.findByTrackingNumberWithLock(event.trackingNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment tracking number", event.trackingNumber()));

        ShipmentStatus current = shipment.getStatus();
        ShipmentStatus next = event.status();

        if (current == next) {
            log.info("Shipment {} received duplicate status event: {}", shipment.getTrackingNumber(), next);
            return mapToShipmentResponse(shipment);
        }

        if (!current.canTransitionTo(next)) {
            throw new BusinessException(
                    "INVALID_STATUS_TRANSITION",
                    "Cannot transition shipment from %s to %s".formatted(current, next),
                    HttpStatus.CONFLICT
            );
        }

        ShipmentEvent milestone = new ShipmentEvent();
        milestone.setFromStatus(current);
        milestone.setToStatus(next);
        milestone.setLocationDescription(event.locationDescription());
        milestone.setCarrierMessage(event.carrierMessage());
        milestone.setEventTimestamp(event.eventTimestamp() != null ? event.eventTimestamp() : Instant.now());
        shipment.addEvent(milestone);
        shipment.setStatus(next);

        if (next == ShipmentStatus.DELIVERED) {
            shipment.setActualDeliveryAt(milestone.getEventTimestamp());
            shipment.setSignedBy(event.signedBy() != null ? event.signedBy() : "Recipient");
            shipment.getSalesOrder().setStatus(SalesOrderStatus.SHIPPED);
            salesOrderRepository.save(shipment.getSalesOrder());
        }

        Shipment updated = shipmentRepository.save(shipment);

        outboxService.recordEvent(
                "SHIPMENT",
                updated.getId().toString(),
                "SHIPMENT_STATUS_UPDATED",
                Map.of(
                        "trackingNumber", updated.getTrackingNumber(),
                        "status", next.name(),
                        "carrier", updated.getCarrier(),
                        "timestamp", milestone.getEventTimestamp().toString()
                )
        );

        return mapToShipmentResponse(updated);
    }

    public LogisticsDto.ShipmentResponse getShipmentByTracking(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber)
                .map(this::mapToShipmentResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment tracking number", trackingNumber));
    }

    private LogisticsDto.ManifestResponse mapToManifestResponse(ShippingManifest m) {
        return new LogisticsDto.ManifestResponse(
                m.getId(),
                m.getManifestNumber(),
                m.getCarrier(),
                m.getWarehouse().getId(),
                m.getStatus(),
                m.getTotalPackages(),
                m.getTotalWeightKg(),
                m.getScheduledPickupAt(),
                m.getClosedAt()
        );
    }

    private LogisticsDto.ShipmentResponse mapToShipmentResponse(Shipment s) {
        return new LogisticsDto.ShipmentResponse(
                s.getId(),
                s.getShipmentNumber(),
                s.getSalesOrder().getId(),
                s.getCarrier(),
                s.getTrackingNumber(),
                s.getServiceLevel(),
                s.getStatus(),
                s.getRecipientName(),
                s.getDestinationAddress(),
                s.getDestinationCity(),
                s.getDestinationCountryCode(),
                s.getEstimatedDeliveryAt(),
                s.getActualDeliveryAt(),
                s.getSignedBy(),
                s.getEvents().stream().map(e -> new LogisticsDto.ShipmentEventResponse(
                        e.getId(),
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getLocationDescription(),
                        e.getCarrierMessage(),
                        e.getEventTimestamp()
                )).toList()
        );
    }
}
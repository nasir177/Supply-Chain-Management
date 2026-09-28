package com.sorascm.backend.logistics.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private ShipmentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private ShipmentStatus toStatus;

    @Column(name = "location_description")
    private String locationDescription;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp = Instant.now();

    @Column(name = "carrier_message", columnDefinition = "TEXT")
    private String carrierMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }
    public ShipmentStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(ShipmentStatus fromStatus) { this.fromStatus = fromStatus; }
    public ShipmentStatus getToStatus() { return toStatus; }
    public void setToStatus(ShipmentStatus toStatus) { this.toStatus = toStatus; }
    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }
    public Instant getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(Instant eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    public String getCarrierMessage() { return carrierMessage; }
    public void setCarrierMessage(String carrierMessage) { this.carrierMessage = carrierMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
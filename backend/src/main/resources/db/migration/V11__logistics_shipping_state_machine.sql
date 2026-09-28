-- Shipping Manifests (Carrier pickup batches)
CREATE TABLE shipping_manifests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manifest_number VARCHAR(64) NOT NULL UNIQUE,
    carrier VARCHAR(64) NOT NULL,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    total_packages INT NOT NULL DEFAULT 0,
    total_weight_kg NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    scheduled_pickup_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_manifest_status CHECK (status IN ('OPEN', 'CLOSED', 'DISPATCHED', 'CANCELLED'))
);

-- Shipments Table (Tracking units assigned to a manifest)
CREATE TABLE shipments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_number VARCHAR(64) NOT NULL UNIQUE,
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id),
    manifest_id UUID REFERENCES shipping_manifests(id),
    carrier VARCHAR(64) NOT NULL,
    tracking_number VARCHAR(128) NOT NULL UNIQUE,
    service_level VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(32) NOT NULL DEFAULT 'MANIFESTED',
    recipient_name VARCHAR(128) NOT NULL,
    destination_address TEXT NOT NULL,
    destination_city VARCHAR(64) NOT NULL,
    destination_country_code CHAR(2) NOT NULL,
    estimated_delivery_at TIMESTAMPTZ,
    actual_delivery_at TIMESTAMPTZ,
    signed_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shipment_status CHECK (status IN (
        'MANIFESTED',
        'TENDERED_TO_CARRIER',
        'IN_TRANSIT',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'FAILED_ATTEMPT',
        'RETURNED',
        'CANCELLED'
    ))
);

-- Shipment Event History (Immutable audit trail of milestones)
CREATE TABLE shipment_events (
    id BIGSERIAL PRIMARY KEY,
    shipment_id UUID NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    location_description VARCHAR(255),
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    carrier_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
CREATE INDEX idx_shipments_so ON shipments(sales_order_id);
CREATE INDEX idx_shipments_manifest ON shipments(manifest_id);
CREATE INDEX idx_shipment_events_shipment ON shipment_events(shipment_id);
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$ BEGIN
    CREATE TYPE location_zone AS ENUM (
        'RECEIVING',
        'GENERAL_STORAGE',
        'COLD_STORAGE',
        'HAZARDOUS',
        'PICKING',
        'PACKING',
        'SHIPPING'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
    CREATE TYPE movement_type AS ENUM (
        'RECEIPT',
        'RESERVATION',
        'RELEASE',
        'PICK',
        'DISPATCH',
        'ADJUSTMENT',
        'TRANSFER'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(64) NOT NULL,
    state_province VARCHAR(64),
    postal_code VARCHAR(32) NOT NULL,
    country_code CHAR(2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_warehouses_code UNIQUE (code)
);

CREATE TABLE warehouse_locations (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    zone location_zone NOT NULL DEFAULT 'GENERAL_STORAGE',
    aisle VARCHAR(16),
    rack VARCHAR(16),
    shelf VARCHAR(16),
    bin VARCHAR(16),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_locations_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE RESTRICT,
    CONSTRAINT uk_locations_warehouse_code UNIQUE (warehouse_id, code)
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    unit_of_measure VARCHAR(16) NOT NULL DEFAULT 'UNIT',
    unit_price NUMERIC(12, 2) NOT NULL,
    reorder_threshold INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_products_reorder_threshold CHECK (reorder_threshold >= 0)
);

CREATE TABLE inventory (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id UUID NOT NULL,
    location_id BIGINT NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_location
        FOREIGN KEY (location_id) REFERENCES warehouse_locations(id) ON DELETE RESTRICT,
    CONSTRAINT uk_inventory_product_location UNIQUE (product_id, location_id),
    CONSTRAINT chk_inventory_on_hand_positive CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reserved_positive CHECK (quantity_reserved >= 0),
    CONSTRAINT chk_inventory_reserved_le_on_hand CHECK (quantity_reserved <= quantity_on_hand)
);

CREATE TABLE inventory_movements (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    movement_type movement_type NOT NULL,
    delta_quantity INT NOT NULL,
    reference_type VARCHAR(64) NOT NULL,
    reference_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movements_inventory
        FOREIGN KEY (inventory_id) REFERENCES inventory(id) ON DELETE RESTRICT,
    CONSTRAINT chk_movements_delta_non_zero CHECK (delta_quantity != 0)
);

CREATE INDEX idx_inventory_product_id ON inventory(product_id);
CREATE INDEX idx_inventory_location_id ON inventory(location_id);
CREATE INDEX idx_movements_inventory_created ON inventory_movements(inventory_id, created_at DESC);
CREATE INDEX idx_movements_reference ON inventory_movements(reference_id) WHERE reference_id IS NOT NULL;
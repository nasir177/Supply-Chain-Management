-- Ensure inventory balances cannot be negative
ALTER TABLE inventory
    DROP CONSTRAINT IF EXISTS chk_inventory_quantities,
    ADD CONSTRAINT chk_inventory_quantities
    CHECK (quantity_on_hand >= 0 AND quantity_reserved >= 0 AND quantity_on_hand >= quantity_reserved);

-- Unique product per location to prevent duplicate tracking rows
ALTER TABLE inventory
    DROP CONSTRAINT IF EXISTS uq_inventory_product_location,
    ADD CONSTRAINT uq_inventory_product_location UNIQUE (product_id, location_id);

-- Enforce strict movement types via constraint
ALTER TABLE inventory_movements
    DROP CONSTRAINT IF EXISTS chk_movement_type,
    ADD CONSTRAINT chk_movement_type
    CHECK (movement_type IN ('INBOUND_RECEIPT', 'OUTBOUND_SHIP', 'ADJUSTMENT_ADD', 'ADJUSTMENT_SUB', 'RESERVATION_HOLD', 'RESERVATION_RELEASE', 'INTERNAL_TRANSFER'));

CREATE INDEX IF NOT EXISTS idx_movements_inventory_id ON inventory_movements(inventory_id);
CREATE INDEX IF NOT EXISTS idx_movements_created_at ON inventory_movements(created_at DESC);
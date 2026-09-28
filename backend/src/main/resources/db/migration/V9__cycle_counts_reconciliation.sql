-- Cycle Count Sessions Table
CREATE TABLE cycle_counts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    count_number VARCHAR(64) NOT NULL UNIQUE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED',
    initiated_by VARCHAR(128) NOT NULL,
    approved_by VARCHAR(128),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_count_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'RECONCILED', 'CANCELLED'))
);

-- Cycle Count Individual Line Items
CREATE TABLE cycle_count_lines (
    id BIGSERIAL PRIMARY KEY,
    cycle_count_id UUID NOT NULL REFERENCES cycle_counts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    location_id BIGINT NOT NULL REFERENCES warehouse_locations(id),
    system_quantity INT NOT NULL,
    counted_quantity INT,
    variance INT GENERATED ALWAYS AS (counted_quantity - system_quantity) STORED,
    variance_value NUMERIC(12, 2),
    counted_by VARCHAR(128),
    counted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cc_warehouse ON cycle_counts(warehouse_id);
CREATE INDEX idx_cc_status ON cycle_counts(status);
CREATE INDEX idx_cc_lines_count ON cycle_count_lines(cycle_count_id);
CREATE INDEX idx_cc_lines_product ON cycle_count_lines(product_id);
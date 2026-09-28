-- Pick Tasks Table
CREATE TABLE pick_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_number VARCHAR(64) NOT NULL UNIQUE,
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    sales_order_line_id BIGINT NOT NULL REFERENCES sales_order_lines(id),
    product_id UUID NOT NULL REFERENCES products(id),
    source_location_id BIGINT NOT NULL REFERENCES warehouse_locations(id),
    quantity_to_pick INT NOT NULL CHECK (quantity_to_pick > 0),
    quantity_picked INT NOT NULL DEFAULT 0 CHECK (quantity_picked >= 0),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    assigned_picker VARCHAR(128),
    picked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pick_task_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_picked_quantity CHECK (quantity_picked <= quantity_to_pick)
);

-- Pack Tasks & Containers Table
CREATE TABLE pack_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pack_number VARCHAR(64) NOT NULL UNIQUE,
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    container_type VARCHAR(64) NOT NULL DEFAULT 'STANDARD_BOX',
    weight_kg NUMERIC(8, 2),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    packer_name VARCHAR(128),
    packed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pack_status CHECK (status IN ('OPEN', 'PACKED', 'CANCELLED'))
);

-- Pack Task Line Items
CREATE TABLE pack_task_lines (
    id BIGSERIAL PRIMARY KEY,
    pack_task_id UUID NOT NULL REFERENCES pack_tasks(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity_packed INT NOT NULL CHECK (quantity_packed > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pick_tasks_so ON pick_tasks(sales_order_id);
CREATE INDEX idx_pick_tasks_status ON pick_tasks(status);
CREATE INDEX idx_pack_tasks_so ON pack_tasks(sales_order_id);
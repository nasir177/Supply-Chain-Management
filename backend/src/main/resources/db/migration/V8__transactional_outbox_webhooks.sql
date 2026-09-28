-- Webhook Subscribers (Target URLs listening to inventory domain events)
CREATE TABLE webhook_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_name VARCHAR(128) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    secret_token VARCHAR(255) NOT NULL,
    subscribed_events VARCHAR(255) NOT NULL, -- e.g. "INVENTORY_ADJUSTED,ORDER_SHIPPED,STOCK_ALERT"
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Transactional Outbox Table
CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL, -- e.g. "INVENTORY", "SALES_ORDER", "ALERT"
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,     -- e.g. "INVENTORY_UPDATED", "STOCK_ALLOCATED"
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX idx_outbox_poll ON outbox_events(status, next_retry_at) WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
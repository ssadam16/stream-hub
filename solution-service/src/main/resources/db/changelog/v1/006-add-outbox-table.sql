--liquibase formatted sql

--changeset nongrataqqq:create-outbox-table
CREATE TABLE outbox_event
(
    id           UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    event_type   VARCHAR(100) NOT NULL,
    aggregate_id UUID         NOT NULL,
    topic        VARCHAR(200) NOT NULL,
    message_key  UUID,
    payload      JSONB        NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    retry_count  INT                   DEFAULT 0,
    next_retry_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_outbox_event_status_created
    ON outbox_event (status, created_at);

CREATE INDEX idx_outbox_event_aggregate_id on outbox_event (aggregate_id);

CREATE INDEX idx_outbox_next_retry
    ON outbox_event(status, next_retry_at);
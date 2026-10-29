CREATE TABLE tickets (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id   BIGINT        NOT NULL REFERENCES users (id),
    assignee_id   BIGINT        REFERENCES users (id),
    title         VARCHAR(200)  NOT NULL,
    description   VARCHAR(5000) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                  CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    -- Filled in by the AI triage (Stage 4); NULL until then.
    category      VARCHAR(30)   CHECK (category IN ('BILLING', 'TECHNICAL', 'ACCOUNT', 'FEATURE_REQUEST', 'OTHER')),
    priority      VARCHAR(20)   CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    sentiment     VARCHAR(20)   CHECK (sentiment IN ('NEGATIVE', 'NEUTRAL', 'POSITIVE')),
    triage_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                  CHECK (triage_status IN ('PENDING', 'DONE', 'FAILED')),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX tickets_customer_id_idx ON tickets (customer_id);
CREATE INDEX tickets_status_created_at_idx ON tickets (status, created_at DESC);

CREATE TABLE ticket_messages (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id  BIGINT        NOT NULL REFERENCES tickets (id),
    author_id  BIGINT        NOT NULL REFERENCES users (id),
    content    VARCHAR(5000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ticket_messages_ticket_id_idx ON ticket_messages (ticket_id, created_at);

CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE refresh_token (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    issued_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);

CREATE TABLE namespace_cost_snapshot (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp                   TIMESTAMP WITH TIME ZONE NOT NULL,
    namespace                   VARCHAR(255) NOT NULL,
    pod_count                   INT NOT NULL,
    cpu_usage_millicores        BIGINT,
    memory_usage_bytes          BIGINT,
    estimated_monthly_cost_usd  NUMERIC(12, 2)
);
CREATE INDEX idx_ns_cost_timestamp ON namespace_cost_snapshot (timestamp DESC);
CREATE INDEX idx_ns_cost_namespace ON namespace_cost_snapshot (namespace, timestamp DESC);

CREATE TABLE alert_rule (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    metric_type         VARCHAR(32) NOT NULL,
    condition           VARCHAR(32) NOT NULL,
    threshold_value     NUMERIC(12, 2) NOT NULL,
    notification_email  VARCHAR(255) NOT NULL,
    last_fired_at       TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE alert_event (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_rule_id UUID NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
    fired_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    metric_value  NUMERIC(12, 2) NOT NULL,
    notified      BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_alert_event_rule ON alert_event (alert_rule_id, fired_at DESC);

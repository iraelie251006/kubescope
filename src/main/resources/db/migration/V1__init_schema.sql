CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE node_snapshot (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_name                VARCHAR(255) NOT NULL,
    instance_type            VARCHAR(64),
    region                   VARCHAR(64),
    cpu_capacity_millicores  BIGINT,
    cpu_usage_millicores     BIGINT,
    memory_capacity_bytes    BIGINT,
    memory_usage_bytes       BIGINT,
    hourly_cost_usd          NUMERIC(10, 4),
    timestamp                TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_node_snapshot_timestamp ON node_snapshot (timestamp DESC);
CREATE INDEX idx_node_snapshot_node_name ON node_snapshot (node_name, timestamp DESC);

CREATE TABLE pod_snapshot (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pod_name                 VARCHAR(255) NOT NULL,
    namespace                VARCHAR(255) NOT NULL,
    node_name                VARCHAR(255),
    cpu_request_millicores   BIGINT,
    memory_request_bytes     BIGINT,
    cpu_usage_millicores     BIGINT,
    memory_usage_bytes       BIGINT,
    phase                    VARCHAR(32),
    timestamp                TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pod_snapshot_timestamp ON pod_snapshot (timestamp DESC);
CREATE INDEX idx_pod_snapshot_namespace ON pod_snapshot (namespace, timestamp DESC);

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL DEFAULT 'ADMIN',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
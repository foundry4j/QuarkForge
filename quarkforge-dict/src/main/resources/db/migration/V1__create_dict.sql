-- QuarkForge Dictionary — v1 schema
-- 多租户隔离（tenant_id）与审计时间戳由框架实体基类自动处理

CREATE TABLE IF NOT EXISTS qf_dict_type
(
    id
    BIGINT
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    64
) NOT NULL,
    code VARCHAR
(
    64
) NOT NULL,
    name VARCHAR
(
    128
) NOT NULL,
    description VARCHAR
(
    512
),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_qf_dict_type_code UNIQUE
(
    tenant_id,
    code
)
    );

CREATE TABLE IF NOT EXISTS qf_dict_item
(
    id
    BIGINT
    PRIMARY
    KEY,
    tenant_id
    VARCHAR
(
    64
) NOT NULL,
    type_code VARCHAR
(
    64
) NOT NULL,
    label VARCHAR
(
    128
) NOT NULL,
    item_value VARCHAR
(
    128
) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parent_value VARCHAR
(
    128
),
    remark VARCHAR
(
    512
),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_qf_dict_item_value UNIQUE
(
    tenant_id,
    type_code,
    item_value
)
    );

CREATE INDEX IF NOT EXISTS idx_qf_dict_item_type
    ON qf_dict_item (type_code, sort_order);

-- H2 / PostgreSQL 都支持上述语法；MySQL 请将 TIMESTAMP 改为 DATETIME(3)

-- QuarkForge Notification — v1 schema
-- 多租户 + 软删除 + 审计时间戳由框架实体基类自动处理

CREATE TABLE IF NOT EXISTS qf_notification
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
    recipient_id VARCHAR
(
    64
) NOT NULL,
    type VARCHAR
(
    32
) NOT NULL,
    title VARCHAR
(
    256
) NOT NULL,
    content TEXT,
    link VARCHAR
(
    512
),
    actor_id VARCHAR
(
    64
),
    read_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_notif_recipient_created
    ON qf_notification (recipient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_recipient_read
    ON qf_notification (recipient_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notif_recipient_delivered
    ON qf_notification (recipient_id, delivered_at);

-- H2 / PostgreSQL 都支持上述语法；MySQL 请将 TIMESTAMP 改为 DATETIME(3)

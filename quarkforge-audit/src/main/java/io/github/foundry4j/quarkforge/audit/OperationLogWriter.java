package io.github.foundry4j.quarkforge.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/// ## OperationLogWriter
/// 操作日志落库。使用 `REQUIRES_NEW` 独立事务：
/// 业务事务因异常回滚时，失败的操作日志仍会保留。
@ApplicationScoped
public class OperationLogWriter {

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void write(OperationLogEntry entry) {
        entry.persist();
    }
}

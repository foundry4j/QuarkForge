package io.github.foundry4j.quarkforge.audit;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/// ## OperationLogRepository
/// 操作日志查询。
///
/// ```java
/// PageResult<OperationLogEntry> page = operationLogRepository.pageByActor("alice", 1, 20);
/// ```
@ApplicationScoped
public class OperationLogRepository extends BaseRepository<OperationLogEntry> {

    /// 按操作人分页查询（按发生时间倒序）。
    public PageResult<OperationLogEntry> pageByActor(String actor, int page, int size) {
        return page(find("actor = ?1 order by occurredAt desc, id desc", actor), page, size);
    }

    /// 按业务模块分页查询（按发生时间倒序）。
    public PageResult<OperationLogEntry> pageByModule(String module, int page, int size) {
        return page(find("module = ?1 order by occurredAt desc, id desc", module), page, size);
    }

    /// 按时间范围分页查询（按发生时间倒序）。
    public PageResult<OperationLogEntry> pageByTimeRange(Instant from, Instant to, int page, int size) {
        return page(find("occurredAt between ?1 and ?2 order by occurredAt desc, id desc", from, to),
                page, size);
    }

    /// 分页查询失败的操作。
    public PageResult<OperationLogEntry> pageFailures(int page, int size) {
        return page(find("success = false order by occurredAt desc, id desc"), page, size);
    }
}

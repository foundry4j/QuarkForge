package io.github.foundry4j.quarkforge.audit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// 测试用操作日志业务方法。
@ApplicationScoped
public class OperationLogFixture {

    @Inject
    OperationLogRepository repository;

    @Transactional
    public void clear() {
        repository.deleteAll();
    }

    @OperationLog(module = "order", action = "create", description = "创建订单")
    public String create(String orderNo, int amount) {
        return "created:" + orderNo;
    }

    @OperationLog(module = "order", action = "delete", recordArgs = false)
    public void remove(String orderNo) {
    }

    @OperationLog(module = "order", action = "pay")
    @Transactional
    public void payAndFail(String orderNo) {
        throw new IllegalStateException("payment failed");
    }

    @OperationLog
    public void plain() {
    }
}

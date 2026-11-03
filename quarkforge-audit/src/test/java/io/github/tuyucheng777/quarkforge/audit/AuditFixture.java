package io.github.tuyucheng777.quarkforge.audit;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/// 测试数据操作，统一在事务中执行。
@ApplicationScoped
public class AuditFixture {

    @Inject
    AuditLogRepository auditLogRepository;

    @Transactional
    public Long create(String name, int price) {
        AuditedItem item = new AuditedItem();
        item.name = name;
        item.price = price;
        item.persist();
        return item.id;
    }

    @Transactional
    public void updatePrice(Long id, int price) {
        AuditedItem item = AuditedItem.findById(id);
        item.price = price;
    }

    @Transactional
    public void remove(Long id) {
        AuditedItem item = AuditedItem.findById(id);
        if (item != null) {
            item.delete();
        }
    }

    @Transactional
    public List<AuditLog> logs(Long entityId) {
        return auditLogRepository.findByEntity(AuditedItem.class.getName(), entityId);
    }

    @Transactional
    public void clear() {
        AuditLog.deleteAll();
        AuditedItem.deleteAll();
    }
}

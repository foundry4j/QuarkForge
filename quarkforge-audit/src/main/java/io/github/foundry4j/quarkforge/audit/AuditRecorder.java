package io.github.foundry4j.quarkforge.audit;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.foundry4j.quarkforge.data.AuditEntityListener;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.github.foundry4j.quarkforge.data.AuditSupport;
import io.github.foundry4j.quarkforge.data.FieldChange;
import io.quarkus.arc.Unremovable;
import io.quarkus.hibernate.orm.panache.Panache;

/// ## AuditRecorder
/// [AuditSupport] 的数据库实现：把 [AuditEntityListener]
/// 采集到的事件写入 [AuditLog]。
///
/// 写入发生在业务事务中，业务回滚时审计记录一并回滚，保证「审计反映真实落库结果」。
///
/// ```properties
/// quarkforge.audit.enabled=true   # 关闭后仅采集不落库
/// ```
@ApplicationScoped
@Unremovable
public class AuditRecorder implements AuditSupport {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AuditActorProvider actorProvider;

    @Inject
    @ConfigProperty(name = "quarkforge.audit.enabled", defaultValue = "true")
    boolean enabled;

    @Override
    public void onCreate(Object entity, Map<String, String> state) {
        if (enabled) {
            write("CREATE", entity, payload(null, state));
        }
    }

    @Override
    public void onUpdate(Object entity, List<FieldChange> changes) {
        if (enabled) {
            write("UPDATE", entity, payload(changes, null));
        }
    }

    @Override
    public void onDelete(Object entity, Map<String, String> state) {
        if (enabled) {
            write("DELETE", entity, payload(null, state));
        }
    }

    private void write(String action, Object entity, String changes) {
        AuditLog log = new AuditLog();
        log.entityType = entity.getClass().getName();
        log.entityId = entityId(entity);
        log.action = action;
        log.actor = actorProvider.currentActor();
        log.changes = changes;
        log.occurredAt = Instant.now();
        Panache.getEntityManager().persist(log);
    }

    private String payload(List<FieldChange> changes, Map<String, String> state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (changes != null) {
            payload.put("changes", changes);
        }
        if (state != null) {
            payload.put("state", state);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return String.valueOf(payload);
        }
    }

    private static String entityId(Object entity) {
        try {
            Field idField = entity.getClass().getField("id");
            Object id = idField.get(entity);
            return id == null ? null : id.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}

package io.github.foundry4j.quarkforge.data;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.panache.Panache;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/// ## AuditEntityListener
/// JPA 实体监听器，挂在 [BaseEntity] 上，为所有继承实体自动采集审计事件：
///
/// - 新建：记录字段快照
/// - 更新：对比加载时快照，产出「价格 100 → 120」这类字段级差异
/// - 硬删除：记录删除前的字段快照
///
/// 事件先进入当前线程的待写队列，在**事务提交前**统一交给 [AuditSupport] 落库。
/// 之所以不直接写入，是因为生命周期回调发生在 Hibernate flush 过程中，
/// 此时再 persist 新实体会修改正在遍历的 ActionQueue，导致删除/新增动作丢失；
/// 统一回写并 flush 可保证审计记录与业务数据处于同一事务。
///
/// 具体落库由 [AuditSupport] 决定；引入 `quarkforge-audit` 后自动实现。
/// 标注 [NotAudited] 的实体（如审计日志自身）会被跳过。
///
/// 批量更新（JPQL `update` / 原生 SQL）不触发生命周期回调，因此不会被审计。
public class AuditEntityListener {

    /// 实体加载时的字段快照，弱引用避免长期持有实体。
    private static final Map<Object, Map<String, String>> SNAPSHOTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /// 当前事务待写入的审计事件。
    private static final ThreadLocal<List<PendingEvent>> PENDING = ThreadLocal.withInitial(ArrayList::new);

    /// 当前事务是否已注册提交回调，避免重复注册。
    private static final ThreadLocal<Boolean> REGISTERED = ThreadLocal.withInitial(() -> false);

    /// 跳过审计的字段：由框架自动维护，属于噪音。
    private static final Set<String> IGNORED_FIELDS = Set.of("createdAt", "updatedAt", "tenantId");

    @PostLoad
    public void postLoad(Object entity) {
        if (auditable(entity)) {
            SNAPSHOTS.put(entity, snapshot(entity));
        }
    }

    @PostPersist
    public void postPersist(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> state = snapshot(entity);
        queue(PendingEvent.Action.CREATE, entity, state, null);
        SNAPSHOTS.put(entity, state);
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> before = SNAPSHOTS.get(entity);
        if (before == null) {
            return;
        }
        List<FieldChange> changes = diff(before, snapshot(entity));
        if (!changes.isEmpty()) {
            queue(PendingEvent.Action.UPDATE, entity, null, changes);
        }
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        if (auditable(entity)) {
            SNAPSHOTS.put(entity, snapshot(entity));
        }
    }

    @PreRemove
    public void preRemove(Object entity) {
        if (!auditable(entity)) {
            return;
        }
        Map<String, String> before = SNAPSHOTS.getOrDefault(entity, snapshot(entity));
        queue(PendingEvent.Action.DELETE, entity, before, null);
    }

    @PostRemove
    public void postRemove(Object entity) {
        SNAPSHOTS.remove(entity);
    }

    private static boolean auditable(Object entity) {
        return entity != null && !entity.getClass().isAnnotationPresent(NotAudited.class);
    }

    private static void queue(PendingEvent.Action action, Object entity,
                              Map<String, String> state, List<FieldChange> changes) {
        PENDING.get().add(new PendingEvent(action, entity, state, changes));
        registerCompletionCallbacks();
    }

    private static void registerCompletionCallbacks() {
        if (Boolean.TRUE.equals(REGISTERED.get())) {
            return;
        }
        try {
            SessionImplementor session = Panache.getEntityManager().unwrap(SessionImplementor.class);
            session.getActionQueue().registerCallback(
                    AuditEntityListener::writePending);
            session.getActionQueue().registerCallback(
                    (_, _) -> discardPending());
            REGISTERED.set(true);
        } catch (RuntimeException ignored) {
            // 没有可用的 Hibernate Session 时退化为不审计，避免影响业务事务
        }
    }

    /// 事务提交前：把排队的事件交给 [AuditSupport]，并立即 flush 让审计记录落库。
    private static void writePending(SharedSessionContractImplementor session) {
        List<PendingEvent> events = PENDING.get();
        if (events.isEmpty()) {
            return;
        }
        List<PendingEvent> batch = new ArrayList<>(events);
        events.clear();

        support().ifPresent(audit -> {
            for (PendingEvent event : batch) {
                switch (event.action()) {
                    case CREATE -> audit.onCreate(event.entity(), event.state());
                    case UPDATE -> audit.onUpdate(event.entity(), event.changes());
                    case DELETE -> audit.onDelete(event.entity(), event.state());
                }
            }
        });

        // 审计记录是在回调中新增的，需要再 flush 一次才会真正写入数据库
        session.flush();
    }

    /// 事务结束（成功或回滚）后清理线程队列，避免污染同一线程上的下一个事务。
    private static void discardPending() {
        PENDING.remove();
        REGISTERED.remove();
    }

    private static Optional<AuditSupport> support() {
        if (Arc.container() == null) {
            return Optional.empty();
        }
        InstanceHandle<AuditSupport> instance = Arc.container().instance(AuditSupport.class);
        return instance.isAvailable() ? Optional.of(instance.get()) : Optional.empty();
    }

    private static Map<String, String> snapshot(Object entity) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Class<?> type = entity.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!inspectable(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    values.put(field.getName(), stringify(field.get(entity)));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // 反射失败时跳过该字段，不影响其他字段审计
                }
            }
        }
        return values;
    }

    private static boolean inspectable(Field field) {
        int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
            return false;
        }
        return !IGNORED_FIELDS.contains(field.getName());
    }

    private static String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static List<FieldChange> diff(Map<String, String> before, Map<String, String> after) {
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());
        List<FieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            String oldValue = before.get(field);
            String newValue = after.get(field);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new FieldChange(field, oldValue, newValue));
            }
        }
        return changes;
    }

    /// 一条待写入的审计事件（在生命周期回调时采集）。
    private record PendingEvent(Action action, Object entity, Map<String, String> state,
                                List<FieldChange> changes) {

        enum Action {
            CREATE, UPDATE, DELETE
        }
    }
}

package io.github.foundry4j.quarkforge.data;

import java.util.List;
import java.util.Map;

/// ## AuditSupport
/// 审计事件 SPI。[AuditEntityListener] 在实体生命周期中调用本接口，
/// 引入 `quarkforge-audit` 模块后自动替换为「字段级差异写入数据库」的实现。
///
/// 未引入审计模块时该接口没有实现，监听器会自动跳过，不产生额外开销。
public interface AuditSupport {

    /// 实体新建后触发，`state` 为新建时的字段快照。
    void onCreate(Object entity, Map<String, String> state);

    /// 实体更新前触发，`changes` 为发生变化的字段列表；无变化时为空列表。
    void onUpdate(Object entity, List<FieldChange> changes);

    /// 实体硬删除前触发，`state` 为删除前的字段快照。
    void onDelete(Object entity, Map<String, String> state);
}

package io.github.foundry4j.quarkforge.audit;

import io.github.foundry4j.quarkforge.core.context.ActorContext;

/// 解析当前操作人。默认实现依次尝试 [ActorContext]、
/// 当前请求的 `SecurityIdentity`，最后回退为 `system`。
///
/// 如需接入其他用户体系，实现本接口并设置为 CDI Bean 即可覆盖默认实现。
public interface AuditActorProvider {

    /// 当前操作人，不应返回 `null`。
    String currentActor();
}

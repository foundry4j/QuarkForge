package io.github.foundry4j.quarkforge.audit;

import jakarta.enterprise.context.ApplicationScoped;
import io.github.foundry4j.quarkforge.core.context.ActorContext;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.security.identity.SecurityIdentity;

/// 默认操作人解析：线程上下文 > 当前请求登录用户 > `system`。
@ApplicationScoped
public class DefaultAuditActorProvider implements AuditActorProvider {

    @Override
    public String currentActor() {
        return ActorContext.current().orElseGet(this::fromSecurityIdentity);
    }

    private String fromSecurityIdentity() {
        if (Arc.container() != null && Arc.container().requestContext().isActive()) {
            InstanceHandle<SecurityIdentity> handle = Arc.container().instance(SecurityIdentity.class);
            if (handle.isAvailable()) {
                SecurityIdentity identity = handle.get();
                if (identity != null && !identity.isAnonymous()) {
                    return identity.getPrincipal().getName();
                }
            }
        }
        return "system";
    }
}

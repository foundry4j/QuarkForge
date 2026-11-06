package io.github.foundry4j.quarkforge.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/// ## RequiresPermissionInterceptor
/// `@RequiresPermission` 的权限校验拦截器，优先级排在角色校验之后。
/// 未登录时由 [AuthContext#currentUser()] 抛出 `401`，权限不足抛出 `403`。
@RequiresPermission
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 11)
public class RequiresPermissionInterceptor {

    @Inject
    AuthContext authContext;

    @AroundInvoke
    Object check(InvocationContext ctx) throws Exception {
        RequiresPermission rule = RbacSupport.resolve(ctx.getMethod(), RequiresPermission.class);
        if (rule == null || rule.value().length == 0) {
            return ctx.proceed();
        }
        LoginUser user = authContext.currentUser();
        RbacSupport.require(user.permissions(), rule.value(), rule.logical(), "权限");
        return ctx.proceed();
    }
}

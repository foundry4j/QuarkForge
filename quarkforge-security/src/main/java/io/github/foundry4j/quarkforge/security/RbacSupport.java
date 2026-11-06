package io.github.foundry4j.quarkforge.security;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/// ## RbacSupport
/// [RequiresRoleInterceptor] 与 [RequiresPermissionInterceptor] 共享的解析与校验逻辑。
final class RbacSupport {

    private RbacSupport() {
    }

    /// 方法级注解优先，方法上没有再看类级。
    static <A extends Annotation> A resolve(Method method, Class<A> type) {
        A annotation = method.getAnnotation(type);
        return annotation != null ? annotation : method.getDeclaringClass().getAnnotation(type);
    }

    /// 按 ANY / ALL 语义校验，不通过时抛出带缺失项说明的 `403 FORBIDDEN`。
    static void require(Set<String> owned, String[] required, Logical logical, String kind) {
        Set<String> ownedOrEmpty = owned == null ? Set.of() : owned;
        boolean pass = logical == Logical.ALL
                ? Arrays.stream(required).allMatch(ownedOrEmpty::contains)
                : Arrays.stream(required).anyMatch(ownedOrEmpty::contains);
        if (!pass) {
            throw new BizException(CommonErrorCode.FORBIDDEN,
                    "缺少" + kind + ": " + String.join(", ", required));
        }
    }
}

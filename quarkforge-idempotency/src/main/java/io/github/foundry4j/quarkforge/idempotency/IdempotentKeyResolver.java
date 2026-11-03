package io.github.foundry4j.quarkforge.idempotency;

import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;

/// ## IdempotentKeyResolver
/// 幂等键解析策略。默认实现 [ExpressionIdempotentKeyResolver] 支持
/// `#参数名.字段` 表达式；如需自定义策略，提供一个同类型的 CDI Bean 覆盖即可。
public interface IdempotentKeyResolver {

    /// 解析幂等键。
    ///
    /// @param context 方法调用上下文
    /// @param config  方法上的 [Idempotent] 注解
    /// @param method  被拦截的方法
    /// @return 幂等键字符串
    String resolve(InvocationContext context, Idempotent config, Method method);
}

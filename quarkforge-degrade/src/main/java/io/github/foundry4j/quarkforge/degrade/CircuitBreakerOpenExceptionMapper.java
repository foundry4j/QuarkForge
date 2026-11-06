package io.github.foundry4j.quarkforge.degrade;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

/// ## CircuitBreakerOpenExceptionMapper
/// 熔断器打开时的快速失败响应：`503 {"code":50300,...}`。
///
/// 与 web 层兜底映射的 `500 服务内部错误` 区分开：熔断是预期内的保护动作，
/// 调用方可据此做降级/排队重试，监控上也不会误报为服务端故障。
@Provider
public class CircuitBreakerOpenExceptionMapper implements ExceptionMapper<CircuitBreakerOpenException> {

    @Override
    public Response toResponse(CircuitBreakerOpenException exception) {
        // 熔断打开属于预期内的保护动作，只记录摘要不打堆栈
        Log.warnf("熔断器已打开，请求被快速拒绝: %s", exception.getMessage());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Result.error(CommonErrorCode.SERVICE_UNAVAILABLE.code(), "服务已熔断，请稍后重试"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

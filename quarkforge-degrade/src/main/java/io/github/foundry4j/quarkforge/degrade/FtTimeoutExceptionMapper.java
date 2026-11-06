package io.github.foundry4j.quarkforge.degrade;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/// ## FtTimeoutExceptionMapper
/// `@Timeout` 触发后的响应：`504 {"code":50400,...}`，
/// 语义上明确是“依赖服务超时”而非本服务故障。
@Provider
public class FtTimeoutExceptionMapper implements ExceptionMapper<TimeoutException> {

    @Override
    public Response toResponse(TimeoutException exception) {
        Log.warnf("调用超时，已被 @Timeout 中断: %s", exception.getMessage());
        return Response.status(Response.Status.GATEWAY_TIMEOUT)
                .entity(Result.error(CommonErrorCode.GATEWAY_TIMEOUT.code(), "依赖服务调用超时，请稍后重试"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

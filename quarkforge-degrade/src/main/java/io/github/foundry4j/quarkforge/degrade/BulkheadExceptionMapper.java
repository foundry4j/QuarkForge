package io.github.foundry4j.quarkforge.degrade;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

/// ## BulkheadExceptionMapper
/// `@Bulkhead` 并发池占满时的快速失败响应：`503 {"code":50300,...}`。
@Provider
public class BulkheadExceptionMapper implements ExceptionMapper<BulkheadException> {

    @Override
    public Response toResponse(BulkheadException exception) {
        Log.warnf("舱壁并发已满，请求被快速拒绝: %s", exception.getMessage());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(Result.error(CommonErrorCode.SERVICE_UNAVAILABLE.code(), "服务并发已满，请稍后重试"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

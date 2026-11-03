package io.github.foundry4j.quarkforge.web.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.core.exception.RateLimitException;
import io.quarkus.logging.Log;

/// ## BizExceptionMapper
/// 将业务异常转换为统一 [Result] 响应，HTTP 状态码由错误码段位推导。
@Provider
public class BizExceptionMapper implements ExceptionMapper<BizException> {

    @Override
    public Response toResponse(BizException exception) {
        int status = HttpStatuses.fromBizCode(exception.code());
        if (!(exception instanceof RateLimitException)) {
            // 4xx 属于预期内的业务分支，只记录一行摘要；5xx 才输出堆栈便于排查
            if (status >= 500) {
                Log.warnf(exception, "业务异常 [%d] %s", exception.code(), exception.getMessage());
            } else {
                Log.warnf("业务异常 [%d] %s", exception.code(), exception.getMessage());
            }
        }
        return Response.status(status)
                .entity(Result.error(exception.code(), exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

package io.github.tuyucheng777.quarkforge.web.exception;

import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.github.tuyucheng777.quarkforge.core.api.CommonErrorCode;
import io.github.tuyucheng777.quarkforge.core.api.Result;

/// ## ValidationExceptionMapper
/// 将 Hibernate Validator 的参数校验失败（`@Valid` / `@NotNull` 等）
/// 转换为 400 + 统一响应体，message 汇总所有字段错误。
@Provider
@Priority(Priorities.USER)
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(this::format)
                .sorted()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = CommonErrorCode.PARAM_ERROR.message();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Result.error(CommonErrorCode.PARAM_ERROR, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private String format(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        String field = dot >= 0 ? path.substring(dot + 1) : path;
        return field + " " + violation.getMessage();
    }
}

package io.github.foundry4j.quarkforge.rpc;

import java.util.Optional;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.ErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

/// ## RpcErrors
/// 业务错误码与 gRPC 状态码的双向映射。
///
/// 约定：服务端抛出的 [BizException] 按其错误码段位映射为 gRPC Status
/// （如 40400 → NOT_FOUND、42900 → RESOURCE_EXHAUSTED），原始错误码以十进制字符串
/// 写入 trailers 的 `x-error-code` 元数据；客户端用 [#toBizException] 还原。
public final class RpcErrors {

    /// 原始错误码元数据键（十进制字符串）。
    public static final Metadata.Key<String> ERROR_CODE_KEY =
            Metadata.Key.of("x-error-code", Metadata.ASCII_STRING_MARSHALLER);

    private RpcErrors() {
    }

    /// 将任意异常转换为 gRPC Status：
    ///
    /// - `StatusException` / `StatusRuntimeException` 原样透传
    /// - [BizException] 按错误码段位映射，message 作为 description
    /// - 其他异常统一 INTERNAL，description 不泄露内部细节
    public static Status toStatus(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            return statusRuntimeException.getStatus();
        }
        if (throwable instanceof StatusException statusException) {
            return statusException.getStatus();
        }
        if (throwable instanceof BizException biz) {
            return Status.fromCode(toStatusCode(biz.code())).withDescription(biz.getMessage());
        }
        return Status.INTERNAL.withDescription(CommonErrorCode.INTERNAL_ERROR.message());
    }

    /// 错误码 → gRPC Status.Code，按 `code / 100` 的 HTTP 语义段位映射。
    public static Status.Code toStatusCode(int errorCode) {
        return switch (errorCode / 100) {
            case 400, 415 -> Status.Code.INVALID_ARGUMENT;
            case 401 -> Status.Code.UNAUTHENTICATED;
            case 403 -> Status.Code.PERMISSION_DENIED;
            case 404 -> Status.Code.NOT_FOUND;
            case 409 -> Status.Code.ABORTED;
            case 413, 429 -> Status.Code.RESOURCE_EXHAUSTED;
            case 500 -> Status.Code.INTERNAL;
            case 503 -> Status.Code.UNAVAILABLE;
            default -> Status.Code.UNKNOWN;
        };
    }

    /// gRPC Status.Code → 通用错误码，trailer 中没有原始错误码时的兜底映射。
    public static ErrorCode toErrorCode(Status.Code statusCode) {
        return switch (statusCode) {
            case INVALID_ARGUMENT, OUT_OF_RANGE, FAILED_PRECONDITION -> CommonErrorCode.PARAM_ERROR;
            case UNAUTHENTICATED -> CommonErrorCode.UNAUTHORIZED;
            case PERMISSION_DENIED -> CommonErrorCode.FORBIDDEN;
            case NOT_FOUND -> CommonErrorCode.NOT_FOUND;
            case ABORTED, ALREADY_EXISTS -> CommonErrorCode.CONFLICT;
            case RESOURCE_EXHAUSTED -> CommonErrorCode.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> CommonErrorCode.SERVICE_UNAVAILABLE;
            default -> CommonErrorCode.INTERNAL_ERROR;
        };
    }

    /// 从 trailers 中提取服务端写入的原始错误码。
    public static Optional<Integer> errorCode(Metadata trailers) {
        if (trailers == null) {
            return Optional.empty();
        }
        String value = trailers.get(ERROR_CODE_KEY);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /// 从调用异常中提取服务端写入的原始错误码。
    public static Optional<Integer> errorCode(StatusRuntimeException exception) {
        return errorCode(exception.getTrailers());
    }

    /// 将调用失败的 [StatusRuntimeException] 还原为 [BizException]：
    /// 优先取 trailers 中的原始错误码，缺失时按 Status.Code 兜底映射。
    public static BizException toBizException(StatusRuntimeException exception) {
        String description = exception.getStatus().getDescription();
        Optional<Integer> code = errorCode(exception);
        if (code.isPresent()) {
            return new BizException(code.get(), description != null ? description : "rpc error");
        }
        ErrorCode fallback = toErrorCode(exception.getStatus().getCode());
        return new BizException(fallback, description != null ? description : fallback.message());
    }
}

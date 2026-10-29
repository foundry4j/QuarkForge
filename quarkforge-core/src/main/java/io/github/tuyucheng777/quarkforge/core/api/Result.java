package io.github.tuyucheng777.quarkforge.core.api;

import java.io.Serializable;

/// ## Result
/// 统一 API 响应体。所有接口返回结构保持一致，便于前端/网关统一处理：
///
/// ```json
/// { "code": 200, "message": "success", "data": ..., "timestamp": 1730000000000 }
/// ```
///
/// @param code      业务状态码，200 表示成功
/// @param message   提示信息
/// @param data      业务数据载荷
/// @param timestamp 服务端响应时间戳（毫秒）
public record Result<T>(int code, String message, T data, long timestamp) implements Serializable {

    /// 成功，无数据载荷。
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /// 成功，携带数据载荷。
    public static <T> Result<T> ok(T data) {
        return new Result<>(CommonErrorCode.SUCCESS.code(), CommonErrorCode.SUCCESS.message(), data,
                System.currentTimeMillis());
    }

    /// 失败，使用错误码的默认提示信息。
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.code(), errorCode.message(), null, System.currentTimeMillis());
    }

    /// 失败，使用自定义提示信息覆盖错误码默认文案。
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.code(), message, null, System.currentTimeMillis());
    }

    /// 失败，直接指定错误码与提示信息。
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    /// 当前响应是否成功。
    public boolean success() {
        return code == CommonErrorCode.SUCCESS.code();
    }
}

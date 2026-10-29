package io.github.tuyucheng777.quarkforge.core.exception;

import io.github.tuyucheng777.quarkforge.core.api.ErrorCode;

/// ## BizException
/// 业务异常基类。web 模块的全局异常处理器会将其转换为
/// [io.github.tuyucheng777.quarkforge.core.api.Result] 响应，HTTP 状态码由
/// [ErrorCode] 段位推导（4xxxx → 对应 4xx，5xxxx → 500）。
public class BizException extends RuntimeException {

    private final transient ErrorCode errorCode;
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.code = errorCode.code();
    }

    public BizException(int code, String message) {
        super(message);
        this.errorCode = null;
        this.code = code;
    }

    /// 错误码数值。
    public int code() {
        return code;
    }

    /// 错误码对象，直接以数值构造时返回 `null`。
    public ErrorCode errorCode() {
        return errorCode;
    }
}

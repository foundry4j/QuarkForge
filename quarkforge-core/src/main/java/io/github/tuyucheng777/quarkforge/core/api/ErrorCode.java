package io.github.tuyucheng777.quarkforge.core.api;

/// ## ErrorCode
/// 统一错误码契约，业务模块可自行实现本接口扩展错误码。
/// 通用错误码见 [CommonErrorCode]。
public interface ErrorCode {

    /// 错误码数值，全局唯一。
    int code();

    /// 默认错误提示信息。
    String message();
}

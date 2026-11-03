package io.github.foundry4j.quarkforge.core.util;

import java.util.Collection;
import java.util.Objects;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.ErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

/// ## Checks
/// 参数与业务前置条件校验工具，校验失败统一抛出 [BizException]（默认 `PARAM_ERROR`），
/// 由 web 层全局异常处理器转换为 400 响应。
public final class Checks {

    private Checks() {
    }

    /// 断言条件为真，否则抛出业务异常。
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BizException(errorCode);
        }
    }

    /// 断言条件为真，否则抛出带自定义信息的业务异常。
    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BizException(errorCode, message);
        }
    }

    /// 断言对象非空，否则抛出 `PARAM_ERROR`。
    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, name + " 不能为空");
        }
        return value;
    }

    /// 断言字符串非空白，否则抛出 `PARAM_ERROR`。
    public static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, name + " 不能为空");
        }
        return value;
    }

    /// 断言集合非空，否则抛出 `PARAM_ERROR`。
    public static <T extends Collection<?>> T notEmpty(T value, String name) {
        if (value == null || value.isEmpty()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, name + " 不能为空");
        }
        return value;
    }

    /// 断言两个对象相等，否则抛出业务异常。
    public static void equals(Object expected, Object actual, ErrorCode errorCode, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new BizException(errorCode, message);
        }
    }
}

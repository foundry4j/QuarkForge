package io.github.foundry4j.quarkforge.core.exception;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;

/// ## RateLimitException
/// 触发限流时抛出，固定映射为 42900 错误码，HTTP 状态 429。
public class RateLimitException extends BizException {

    public RateLimitException() {
        super(CommonErrorCode.TOO_MANY_REQUESTS);
    }

    public RateLimitException(String message) {
        super(CommonErrorCode.TOO_MANY_REQUESTS, message);
    }
}

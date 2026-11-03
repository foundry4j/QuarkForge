package io.github.foundry4j.quarkforge.web.exception;

final class HttpStatuses {

    private HttpStatuses() {
    }

    /// 由业务错误码推导 HTTP 状态码：错误码前三位落在 [400, 599] 区间时对齐到对应
    /// HTTP 状态（如 42900 → 429），其余业务错误统一返回 200，由响应体中的 `code` 区分。
    static int fromBizCode(int code) {
        int guess = code / 100;
        return guess >= 400 && guess < 600 ? guess : 200;
    }
}

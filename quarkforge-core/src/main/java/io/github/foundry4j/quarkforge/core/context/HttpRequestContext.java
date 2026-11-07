package io.github.foundry4j.quarkforge.core.context;

import java.util.Optional;

/// ## HttpRequestContext
/// 当前 HTTP 请求的轻量快照，包含请求方法、路径、客户端 IP、User-Agent 与
/// traceId。由 web 层在请求入口写入，供操作日志等能力读取；非 web 场景下为空。
///
/// 线程绑定的值在请求结束时必须清理，避免线程复用造成串号。
public final class HttpRequestContext {

    /// 请求快照。字段可能为 `null`（如无代理头时缺少 IP）。
    public record Info(String method, String path, String ip, String userAgent, String traceId) {
    }

    private static final ThreadLocal<Info> CURRENT = new ThreadLocal<>();

    private HttpRequestContext() {
    }

    /// 写入当前请求快照。
    public static void set(Info info) {
        if (info == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(info);
        }
    }

    /// 当前请求快照，非 web 请求时为空。
    public static Optional<Info> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /// 清除当前请求快照。
    public static void clear() {
        CURRENT.remove();
    }
}

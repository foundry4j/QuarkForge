package io.github.foundry4j.quarkforge.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/// ## SingleFlightGate
/// per-key 单飞门闩（Single Flight）：并发调用在首个线程上排队，
/// 其他线程等待同一个 `CompletableFuture`，避免热点 Key 大量并发穿透到下游。
///
/// 线程模型：
///
/// ```
///   T1 ── get ── miss ── put F1 ── load ── complete ── 返回 value
///   T2 ── get ── miss ── 发现 F1 ──── .get() 等待 ────── 拿到同一个 value
///   T3 ── get ── miss ── 发现 F1 ──── .get() 等待 ────── 拿到同一个 value
/// ```
///
/// 如果加载失败（抛出异常），该 future 会被从注册表删除，后续请求重新尝试加载。
///
/// 本实现仅覆盖单 JVM 场景；多节点部署下的分布式单飞请引入 `quarkforge-redis` 后，
/// 用 `RedisLockService` 替换此实现。
public final class SingleFlightGate {

    private final ConcurrentMap<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();

    /// 尝试为 `key` 注册一次新的加载：
    /// - 返回值的 `future` 是 in-flight future（已有进行中则返回同一个）
    /// - `iAmRegistrant` 为 `true` 表示当前线程是首次注册者，应当执行 loader
    public Registration tryRegister(String key) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        CompletableFuture<Object> previous = inFlight.putIfAbsent(key, future);
        if (previous == null) {
            return new Registration(future, true);
        }
        return new Registration(previous, false);
    }

    /// 便利方法：只取 future（不区分是否注册者）。
    public CompletableFuture<Object> future(String key) {
        var existing = inFlight.get(key);
        if (existing != null) {
            return existing;
        }
        // 创建一个新的占位 future（极少发生，多线程竞争 race 外不应走到）
        return tryRegister(key).future();
    }

    /// 完成加载并返回结果（允许为 `NullValue.INSTANCE` 表示空值缓存）。
    public void complete(String key, Object result) {
        CompletableFuture<Object> future = inFlight.remove(key);
        if (future != null && !future.isDone()) {
            future.complete(result);
        }
    }

    /// 加载失败：future 正常完成但返回 null（调用方会把 null 当作失败处理），
    /// 并从注册表移除，后续请求可重新尝试。
    public void fail(String key, Throwable error) {
        CompletableFuture<Object> future = inFlight.remove(key);
        if (future != null && !future.isDone()) {
            future.completeExceptionally(error);
        }
    }

    /// 当前有多少个 in-flight 键（调试用）。
    public int inFlightCount() {
        return inFlight.size();
    }

    /// [tryRegister] 的返回值。
    public record Registration(CompletableFuture<Object> future, boolean iAmRegistrant) {
    }
}

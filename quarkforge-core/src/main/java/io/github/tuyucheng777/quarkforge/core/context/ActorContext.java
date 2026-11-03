package io.github.tuyucheng777.quarkforge.core.context;

import java.util.Optional;
import java.util.function.Supplier;

/// ## ActorContext
/// 当前操作人上下文，供审计日志、软删除等能力记录「谁做的」。
///
/// Web 场景通常由安全模块在请求入口调用 [set]，或使用 [runAs] / [callAs] 包裹业务逻辑：
///
/// ```java
/// ActorContext.callAs("alice", () -> userService.updatePrice(id, 120));
/// ```
///
/// 未设置时返回空，调用方可回退为 `system`。
public final class ActorContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private ActorContext() {
    }

    /// 设置当前操作人，`null` 或空白等价于清除。
    public static void set(String actor) {
        if (actor == null || actor.isBlank()) {
            CURRENT.remove();
        } else {
            CURRENT.set(actor);
        }
    }

    /// 当前操作人，未设置时为空。
    public static Optional<String> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /// 当前操作人，未设置时返回 `fallback`。
    public static String currentOr(String fallback) {
        String actor = CURRENT.get();
        return actor == null || actor.isBlank() ? fallback : actor;
    }

    /// 在指定操作人下执行有返回值的逻辑，结束后恢复原值（支持嵌套）。
    public static <T> T callAs(String actor, Supplier<T> action) {
        String previous = CURRENT.get();
        set(actor);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    /// 在指定操作人下执行逻辑，结束后恢复原值（支持嵌套）。
    public static void runAs(String actor, Runnable action) {
        callAs(actor, () -> {
            action.run();
            return null;
        });
    }

    /// 清除当前线程的操作人。
    public static void clear() {
        CURRENT.remove();
    }

    private static void restore(String previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}

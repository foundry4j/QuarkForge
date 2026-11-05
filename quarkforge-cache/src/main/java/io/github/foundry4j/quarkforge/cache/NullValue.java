package io.github.foundry4j.quarkforge.cache;

/// ## NullValue
/// `@Cacheable` / `SingleFlightGate` 场景下用于缓存方法返回的 `null` 值。
///
/// 不向业务代码暴露；拦截器在写入缓存时把 `null` 替换为此单例，
/// 读取时再还原，配合较短 TTL 防御缓存穿透。
public final class NullValue {

    public static final NullValue INSTANCE = new NullValue();

    private NullValue() {
    }

    /// 判断缓存值是否代表空值。
    public static boolean isNull(Object value) {
        return value == INSTANCE;
    }
}

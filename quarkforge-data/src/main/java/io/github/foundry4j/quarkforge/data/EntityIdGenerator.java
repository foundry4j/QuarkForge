package io.github.foundry4j.quarkforge.data;

import org.eclipse.microprofile.config.ConfigProvider;
import io.github.foundry4j.quarkforge.core.util.SnowflakeIdGenerator;

/// ## EntityIdGenerator
/// 实体主键生成器持有者。懒加载单例，节点 ID 读取配置 `quarkforge.data.worker-id`，
/// 未配置时默认为 0。多实例部署同一数据源时应为每个实例配置不同的 worker-id。
final class EntityIdGenerator {

    private static volatile SnowflakeIdGenerator generator;

    private EntityIdGenerator() {
    }

    static long next() {
        SnowflakeIdGenerator current = generator;
        if (current == null) {
            synchronized (EntityIdGenerator.class) {
                current = generator;
                if (current == null) {
                    long workerId = ConfigProvider.getConfig()
                            .getOptionalValue("quarkforge.data.worker-id", Long.class)
                            .orElse(0L);
                    current = new SnowflakeIdGenerator(workerId);
                    generator = current;
                }
            }
        }
        return current.nextId();
    }
}

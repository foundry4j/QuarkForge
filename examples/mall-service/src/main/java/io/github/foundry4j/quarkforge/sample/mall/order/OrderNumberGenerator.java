package io.github.foundry4j.quarkforge.sample.mall.order;

import io.github.foundry4j.quarkforge.core.util.SnowflakeIdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/// ## OrderNumberGenerator
/// 基于雪花 ID 的订单号生成器。
@ApplicationScoped
public class OrderNumberGenerator {

    private final SnowflakeIdGenerator generator;

    @Inject
    public OrderNumberGenerator(@ConfigProperty(name = "quarkforge.data.worker-id", defaultValue = "0") long workerId) {
        this.generator = new SnowflakeIdGenerator(workerId);
    }

    /// 生成形如 `SO123456789` 的订单号。
    public String next() {
        return "SO" + generator.nextId();
    }
}

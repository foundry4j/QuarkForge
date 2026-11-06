package io.github.foundry4j.quarkforge.messaging.rabbitmq;

import io.github.foundry4j.quarkforge.messaging.MessageEnvelope;
import io.github.foundry4j.quarkforge.messaging.MessageSubscriber;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

/// ## RabbitMqMessageSubscriber
/// RabbitMQ 消费端，[MessageSubscriber] 的生产实现（替换本地默认实现）：
///
/// 1. 声明持久化队列 `queue-prefix + topic`；
/// 2. 配置了 exchange 时按 `topic` 作为 routing key 绑定（与发布侧对称）；
/// 3. 手动确认：处理成功 `basicAck`，处理器抛异常 `basicNack` 且不入队重投，
///    避免毒消息阻塞队列（需要重试/死信的业务可在队列上自行配置 DLX）；
/// 4. 连接开启自动恢复，代理重启后订阅自动重建。
///
/// 配置与发布侧共享连接参数：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.messaging.rabbitmq.uri` | `amqp://guest:guest@localhost:5672` | 连接串 |
/// | `quarkforge.messaging.rabbitmq.exchange` | 空 | 交换机，空则使用默认交换机直发队列 |
/// | `quarkforge.messaging.rabbitmq.queue-prefix` | `quarkforge.` | 消费队列名前缀 |
@ApplicationScoped
public class RabbitMqMessageSubscriber implements MessageSubscriber {

    private static final Logger LOG = Logger.getLogger(RabbitMqMessageSubscriber.class);

    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.uri", defaultValue = "amqp://guest:guest@localhost:5672")
    String uri;

    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.exchange", defaultValue = "")
    String exchange;

    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.queue-prefix", defaultValue = "quarkforge.")
    String queuePrefix;

    @Inject
    Vertx vertx;

    private RabbitMQClient client;

    @PostConstruct
    void init() {
        RabbitMQOptions options = new RabbitMQOptions()
                .setUri(uri)
                .setAutomaticRecoveryEnabled(true);
        client = RabbitMQClient.create(vertx, options);
    }

    /// 建立一条订阅：连接 → 声明队列 → 按需绑定 → 手动确认消费。
    @Override
    public CompletionStage<Void> subscribe(String topic, Consumer<MessageEnvelope> handler) {
        String queue = queuePrefix + topic;
        Future<Void> setup = client.queueDeclare(queue, true, false, false)
                .compose(_ -> bindIfNeeded(queue, topic))
                .compose(_ -> client.basicConsumer(queue, new QueueOptions().setAutoAck(false)))
                .map(consumer -> {
                    consumer.handler(delivery -> handle(topic, delivery, handler));
                    return null;
                });
        return setup.toCompletionStage();
    }

    private Future<Void> bindIfNeeded(String queue, String topic) {
        if (exchange == null || exchange.isBlank()) {
            return Future.succeededFuture();
        }
        return client.queueBind(queue, exchange, topic);
    }

    private void handle(String topic, io.vertx.rabbitmq.RabbitMQMessage delivery,
                        Consumer<MessageEnvelope> handler) {
        long deliveryTag = delivery.envelope().getDeliveryTag();
        try {
            byte[] body = delivery.body() == null ? new byte[0] : delivery.body().getBytes();
            handler.accept(RabbitMqMessages.toEnvelope(topic, body, delivery.properties()));
            client.basicAck(deliveryTag, false);
        } catch (Exception e) {
            LOG.warnf(e, "消息处理失败，nack 不入队 topic=%s", topic);
            client.basicNack(deliveryTag, false, false);
        }
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.stop();
        }
    }
}

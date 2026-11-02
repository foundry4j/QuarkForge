package io.github.tuyucheng777.quarkforge.messaging.rabbitmq;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.github.tuyucheng777.quarkforge.messaging.MessageEnvelope;
import io.github.tuyucheng777.quarkforge.messaging.MessagePublisher;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

/// ## RabbitMqMessagePublisher
/// RabbitMQ 消息发布实现，启动时创建 Vert.x RabbitMQ 客户端，并按需声明交换机。
///
/// 配置项：
///
/// ```properties
/// quarkforge.messaging.rabbitmq.uri=amqp://guest:guest@localhost:5672/
/// quarkforge.messaging.rabbitmq.exchange=quarkforge.topic   # 留空则投递到默认交换机（routingKey 即队列名）
/// quarkforge.messaging.rabbitmq.exchange-type=topic
/// quarkforge.messaging.rabbitmq.declare-exchange=true
/// quarkforge.messaging.rabbitmq.publisher-confirms=false    # 开启后等待 broker 确认，吞吐略降但更可靠
/// ```
///
/// `destination` 作为 routing key；topic 交换机下可用 `order.created` 等通配规则绑定队列。
@ApplicationScoped
public class RabbitMqMessagePublisher implements MessagePublisher {

    private static final Logger LOG = Logger.getLogger(RabbitMqMessagePublisher.class);

    @Inject
    Vertx vertx;

    @Inject
    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.uri", defaultValue = "amqp://guest:guest@localhost:5672/")
    String uri;

    @Inject
    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.exchange", defaultValue = "")
    String exchange;

    @Inject
    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.exchange-type", defaultValue = "topic")
    String exchangeType;

    @Inject
    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.declare-exchange", defaultValue = "true")
    boolean declareExchange;

    @Inject
    @ConfigProperty(name = "quarkforge.messaging.rabbitmq.publisher-confirms", defaultValue = "false")
    boolean publisherConfirms;

    private final Object setupLock = new Object();

    private CompletableFuture<Void> setup;

    private RabbitMQClient client;

    @PostConstruct
    void init() {
        client = RabbitMQClient.create(vertx, new RabbitMQOptions().setUri(uri));
    }

    @Override
    public CompletionStage<Void> publish(MessageEnvelope message) {
        Objects.requireNonNull(message, "message");
        if (message.destination() == null || message.destination().isBlank()) {
            throw new IllegalArgumentException("消息 destination 不能为空");
        }
        return ensurePrepared().thenCompose(ignored -> send(message));
    }

    /// 懒初始化：首次投递时确认 publisher confirms 与声明交换机。
    /// 初始化失败不缓存，下次投递会自动重试，避免 MQ 启动晚于应用时永久不可用。
    private CompletionStage<Void> ensurePrepared() {
        CompletableFuture<Void> current;
        synchronized (setupLock) {
            if (setup == null || setup.isCompletedExceptionally()) {
                setup = prepare();
            }
            current = setup;
        }
        return current;
    }

    private CompletableFuture<Void> prepare() {
        CompletableFuture<Void> prepared = new CompletableFuture<>();
        Future<Void> stage = publisherConfirms ? client.confirmSelect() : Future.succeededFuture();
        if (declareExchange && exchange != null && !exchange.isBlank()) {
            stage = stage.compose(ignored -> client.exchangeDeclare(exchange, exchangeType, true, false));
        }
        stage.onComplete(result -> {
            if (result.succeeded()) {
                prepared.complete(null);
            } else {
                LOG.warnf(result.cause(), "RabbitMQ 初始化失败，稍后重试 uri=%s exchange=%s", uri, exchange);
                prepared.completeExceptionally(result.cause());
            }
        });
        return prepared;
    }

    private CompletionStage<Void> send(MessageEnvelope message) {
        String routingKey = message.destination();
        Future<Void> published = client.basicPublish(
                exchange,
                routingKey,
                RabbitMqMessages.toProperties(message),
                Buffer.buffer(message.payload()));
        if (publisherConfirms) {
            published = published.compose(ignored -> client.waitForConfirms());
        }
        return published.toCompletionStage();
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.stop();
        }
    }
}

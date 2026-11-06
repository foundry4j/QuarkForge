package io.github.foundry4j.quarkforge.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/// ## MessageConsumerInitializer
/// 启动时扫描所有实现 [MessageHandler] 且标注 [MessageConsumer] 的 Bean，
/// 统一向 [MessageSubscriber] 注册订阅，并把信封负载反序列化为
/// 注解声明的 `payloadType` 后再回调业务处理器。
///
/// 订阅失败（如 MQ 尚未就绪）只记录错误，不阻断应用启动；
/// 单个消费者的反序列化/业务异常也不会影响其他消费者。
@ApplicationScoped
public class MessageConsumerInitializer {

    private static final Logger LOG = Logger.getLogger(MessageConsumerInitializer.class);

    @Inject
    MessageSubscriber subscriber;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    BeanManager beanManager;

    /// 仅用于阻止 Arc 构建期移除未被注入的消费者 Bean
    /// （消费者靠本类反射式扫描注册，Arc 静态分析看不到引用）。
    @Inject
    Instance<MessageHandler<?>> consumerBeans;

    void onStart(@Observes StartupEvent event) {
        // 扫描全部 Bean 再过滤：Arc 对“原始类型接口 + 泛型实现类”的按类型解析不可靠
        for (Bean<?> bean : beanManager.getBeans(Object.class, Any.Literal.INSTANCE)) {
            Class<?> beanClass = bean.getBeanClass();
            if (!MessageHandler.class.isAssignableFrom(beanClass)) {
                continue;
            }
            MessageConsumer config = beanClass.getAnnotation(MessageConsumer.class);
            if (config == null) {
                continue;
            }
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
            MessageHandler<Object> handler = (MessageHandler<Object>) beanManager.getReference(bean, beanClass, creationalContext);
            subscribe(config, handler);
        }
    }

    private void subscribe(MessageConsumer config, MessageHandler<Object> handler) {
        subscriber.subscribe(config.topic(), envelope -> {
            Object payload = readPayload(envelope.payload(), config.payloadType());
            handler.onMessage(payload, envelope);
        }).whenComplete((_, error) -> {
            if (error != null) {
                LOG.errorf(error, "消息订阅建立失败 topic=%s consumer=%s",
                        config.topic(), handler.getClass().getName());
            } else {
                LOG.infof("消息订阅已建立 topic=%s consumer=%s", config.topic(), handler.getClass().getName());
            }
        });
    }

    /// `String` 类型原样返回 JSON 文本，其余按 Jackson 反序列化。
    private Object readPayload(String json, Class<?> payloadType) {
        if (payloadType == String.class) {
            return json;
        }
        try {
            return objectMapper.readValue(json, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("消息负载反序列化失败: " + payloadType.getName(), e);
        }
    }
}

package io.github.tuyucheng777.quarkforge.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

/// ## ExpressionIdempotentKeyResolverTest
/// 验证默认键解析器对 `#参数名`、`#p0`、`.字段` 链以及默认序列化键的处理。
class ExpressionIdempotentKeyResolverTest {

    private ExpressionIdempotentKeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ExpressionIdempotentKeyResolver(new ObjectMapper());
    }

    @Test
    void resolvesNamedParameter() throws Exception {
        Method method = Service.class.getMethod("create", String.class, int.class);
        Idempotent config = annotation("order:#orderNo");
        String key = resolver.resolve(ctx(method, new Object[]{"A001", 2}), config, method);
        assertTrue(key.endsWith(":order:A001"), key);
    }

    @Test
    void resolvesIndexedParameterAndNestedField() throws Exception {
        Method method = Service.class.getMethod("update", Item.class);
        Idempotent config = annotation("item:#p0.id:#p0.name");
        Item item = new Item();
        item.id = 42L;
        item.name = "widget";
        String key = resolver.resolve(ctx(method, new Object[]{item}), config, method);
        assertTrue(key.endsWith(":item:42:widget"), key);
    }

    @Test
    void fallsBackToSerializedArgsWhenKeyBlank() throws Exception {
        Method method = Service.class.getMethod("create", String.class, int.class);
        Idempotent config = annotation("");
        String key = resolver.resolve(ctx(method, new Object[]{"A001", 2}), config, method);
        assertTrue(key.contains("A001"), key);
    }

    private Idempotent annotation(String key) {
        return new Idempotent() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Idempotent.class;
            }

            @Override
            public String key() {
                return key;
            }

            @Override
            public long ttlSeconds() {
                return 600;
            }
        };
    }

    private InvocationContext ctx(Method method, Object[] args) {
        return new InvocationContext() {
            @Override
            public Object getTarget() {
                return null;
            }

            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Object[] getParameters() {
                return args;
            }

            @Override
            public void setParameters(Object[] params) {
            }

            @Override
            public Map<String, Object> getContextData() {
                return Map.of();
            }

            @Override
            public Object proceed() {
                return null;
            }

            @Override
            public Object getTimer() {
                return null;
            }

            @Override
            public java.lang.reflect.Constructor<?> getConstructor() {
                return null;
            }
        };
    }

    static class Service {
        public String create(String orderNo, int qty) {
            return orderNo;
        }

        public String update(Item item) {
            return item.name;
        }
    }

    public static class Item {
        public Long id;
        public String name;
    }
}

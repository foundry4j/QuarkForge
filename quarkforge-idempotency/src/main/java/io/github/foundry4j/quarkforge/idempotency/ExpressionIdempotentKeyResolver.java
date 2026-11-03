package io.github.foundry4j.quarkforge.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.interceptor.InvocationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// ## ExpressionIdempotentKeyResolver
/// 默认幂等键解析器。
///
/// - 配置了 `key` 表达式时，识别 `#参数名`（或 `#p0` 按索引）占位符，并支持 `.字段` 链式取值；
/// - 未配置表达式时，使用方法参数的 JSON 序列化结果作为键的一部分。
@DefaultBean
@ApplicationScoped
public class ExpressionIdempotentKeyResolver implements IdempotentKeyResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("#([\\w]+)(\\.[\\w]+)*");

    private final ObjectMapper objectMapper;

    @Inject
    public ExpressionIdempotentKeyResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String resolve(InvocationContext context, Idempotent config, Method method) {
        String base = method.getDeclaringClass().getName() + "#" + method.getName();
        Object[] args = context.getParameters();
        if (config.key() == null || config.key().isBlank()) {
            return base + ":" + defaultKeyMaterial(method, args);
        }
        return base + ":" + resolveExpression(config.key(), method.getParameters(), args);
    }

    private String resolveExpression(String expression, Parameter[] parameters, Object[] args) {
        Map<String, Object> nameIndex = buildNameIndex(parameters, args);
        Matcher matcher = PLACEHOLDER.matcher(expression);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group();
            String value = resolveToken(token, parameters, args, nameIndex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Map<String, Object> buildNameIndex(Parameter[] parameters, Object[] args) {
        return java.util.stream.IntStream.range(0, parameters.length)
                .boxed()
                .collect(java.util.stream.Collectors.toMap(
                        i -> parameters[i].getName(),
                        i -> args[i],
                        (_, b) -> b));
    }

    private String resolveToken(String token, Parameter[] parameters, Object[] args, Map<String, Object> nameIndex) {
        String withoutHash = token.substring(1);
        String[] parts = withoutHash.split("\\.");
        String head = parts[0];
        Object value;
        if (head.startsWith("p") && head.length() > 1 && isDigits(head.substring(1))) {
            int index = Integer.parseInt(head.substring(1));
            value = index >= 0 && index < args.length ? args[index] : null;
        } else {
            value = nameIndex.get(head);
        }
        try {
            JsonNode node = objectMapper.valueToTree(value);
            for (int i = 1; i < parts.length; i++) {
                if (node == null || node.isNull()) {
                    return "null";
                }
                node = node.get(parts[i]);
            }
            return node == null || node.isNull() ? "null" : node.asText();
        } catch (IllegalArgumentException e) {
            return String.valueOf(value);
        }
    }

    private boolean isDigits(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String defaultKeyMaterial(Method method, Object[] args) {
        try {
            return objectMapper.writeValueAsString(Arrays.asList(args));
        } catch (JsonProcessingException e) {
            return Arrays.deepHashCode(args) + "";
        }
    }
}

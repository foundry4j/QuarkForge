package io.github.foundry4j.quarkforge.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// ## LockKeyResolver
/// `@DistributedLock` 的锁键模板解析。占位符语义与幂等键解析保持一致：
///
/// - `#参数名`：按编译期保留的参数名取值；
/// - `#p0`、`#p1`：按参数下标取值；
/// - `.字段`：对取值结果做链式字段访问（经 Jackson 转为树结构）；
/// - 模板中的普通文本原样保留，可与占位符混排（如 `t:#req.tenantId:o:#req.id`）。
final class LockKeyResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("#([\\w]+)(\\.[\\w]+)*");

    private LockKeyResolver() {
    }

    /// 解析锁键；模板为空时退化为 `类全名#方法名`。
    static String resolve(String template, Method method, Object[] args, ObjectMapper objectMapper) {
        if (template == null || template.isBlank()) {
            return method.getDeclaringClass().getName() + "#" + method.getName();
        }
        Parameter[] parameters = method.getParameters();
        Map<String, Object> nameIndex = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            nameIndex.put(parameters[i].getName(), args[i]);
        }

        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String value = resolveToken(matcher.group(), args, nameIndex, objectMapper);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String resolveToken(String token, Object[] args, Map<String, Object> nameIndex,
                                       ObjectMapper objectMapper) {
        String[] parts = token.substring(1).split("\\.");
        Object value;
        String head = parts[0];
        if (head.length() > 1 && head.charAt(0) == 'p' && isDigits(head.substring(1))) {
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

    private static boolean isDigits(String s) {
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
}

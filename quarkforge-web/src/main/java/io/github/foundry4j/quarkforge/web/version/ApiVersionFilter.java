package io.github.foundry4j.quarkforge.web.version;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.Result;

/// API 版本过滤器，负责三件事：
///
/// 1. 解析请求版本：优先 `X-API-Version` 请求头，其次 URL 首段 `/vN`，
///    最后回退到 `quarkforge.web.version.default`；
/// 2. 校验版本：标注了 [ApiVersion] 的接口，若请求版本不在支持列表内，
///    返回 `400 {"code":40001,...}`；
/// 3. 回写响应头：`X-API-Version`，废弃接口额外附加 `Deprecation` /
///    `Sunset` / `Link`。
///
/// 配置项：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.web.version.enabled` | `true` | 是否启用版本处理 |
/// | `quarkforge.web.version.default` | `v1` | 未指定版本时使用的默认版本 |
@ApplicationScoped
public class ApiVersionFilter {

    public static final String VERSION_HEADER = "X-API-Version";
    public static final String DEPRECATION_HEADER = "Deprecation";
    public static final String SUNSET_HEADER = "Sunset";

    @ConfigProperty(name = "quarkforge.web.version.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "quarkforge.web.version.default", defaultValue = "v1")
    String defaultVersion;

    @Inject
    ApiVersionContext versionContext;

    @ServerRequestFilter
    public void resolveVersion(ContainerRequestContext requestContext) {
        if (!enabled) {
            return;
        }
        versionContext.setRequested(normalize(resolveVersionValue(requestContext)));
    }

    @ServerResponseFilter
    public void applyVersion(ContainerRequestContext requestContext,
                             ContainerResponseContext responseContext,
                             ResourceInfo resourceInfo) {
        if (!enabled || resourceInfo == null) {
            return;
        }
        String requested = normalize(versionContext.requested());
        responseContext.getHeaders().putSingle(VERSION_HEADER, requested);

        ApiVersion apiVersion = findAnnotation(resourceInfo);
        if (apiVersion == null) {
            return;
        }
        List<String> supported = Arrays.stream(apiVersion.value())
                .map(ApiVersionFilter::normalize)
                .toList();
        if (!supported.contains(requested)) {
            responseContext.setStatus(CommonErrorCode.API_VERSION_UNSUPPORTED.code() / 100);
            responseContext.setEntity(Result.error(CommonErrorCode.API_VERSION_UNSUPPORTED,
                    "不支持的 API 版本: " + requested + "，支持: " + String.join(", ", supported)));
            responseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            return;
        }
        markDeprecated(responseContext, resourceInfo, apiVersion);
    }

    private void markDeprecated(ContainerResponseContext responseContext,
                                ResourceInfo resourceInfo, ApiVersion apiVersion) {
        Method method = resourceInfo.getResourceMethod();
        boolean deprecated = apiVersion.deprecated()
                || (method != null && method.isAnnotationPresent(Deprecated.class))
                || (resourceInfo.getResourceClass() != null
                        && resourceInfo.getResourceClass().isAnnotationPresent(Deprecated.class));
        if (!deprecated) {
            return;
        }
        responseContext.getHeaders().putSingle(DEPRECATION_HEADER, "true");
        if (!apiVersion.sunset().isBlank()) {
            responseContext.getHeaders().putSingle(SUNSET_HEADER, apiVersion.sunset());
        }
        if (!apiVersion.successor().isBlank()) {
            responseContext.getHeaders().putSingle("Link",
                    "<" + apiVersion.successor() + ">; rel=\"successor-version\"");
        }
    }

    /// 请求头优先；其次识别 URL 首段 `vN`；否则用默认版本。
    private String resolveVersionValue(ContainerRequestContext requestContext) {
        String header = requestContext.getHeaderString(VERSION_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String path = requestContext.getUriInfo().getPath();
        if (path != null) {
            String first = path.startsWith("/") ? path.substring(1) : path;
            int slash = first.indexOf('/');
            String segment = slash >= 0 ? first.substring(0, slash) : first;
            if (segment.matches("(?i)v?\\d+")) {
                return segment;
            }
        }
        return defaultVersion;
    }

    /// 统一版本写法：`1` / `V1` → `v1`；其他值原样小写返回，便于报错提示。
    static String normalize(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String trimmed = version.trim().toLowerCase(Locale.ROOT);
        return trimmed.matches("\\d+") ? "v" + trimmed : trimmed;
    }

    private ApiVersion findAnnotation(ResourceInfo resourceInfo) {
        Method method = resourceInfo.getResourceMethod();
        if (method != null) {
            ApiVersion onMethod = method.getAnnotation(ApiVersion.class);
            if (onMethod != null) {
                return onMethod;
            }
        }
        Class<?> resourceClass = resourceInfo.getResourceClass();
        return resourceClass == null ? null : resourceClass.getAnnotation(ApiVersion.class);
    }
}

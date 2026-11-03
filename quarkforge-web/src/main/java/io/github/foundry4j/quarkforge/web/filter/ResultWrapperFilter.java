package io.github.foundry4j.quarkforge.web.filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.github.foundry4j.quarkforge.web.annotation.RawResponse;

/// ## ResultWrapperFilter
/// 统一响应体自动包装：资源方法无需手动包 `Result`，返回的业务对象会被自动
/// 包装为 `{"code":200,"message":"success","data":...}` 结构。
///
/// 跳过包装的情形：
///
/// - 方法或类标注了 [RawResponse]
/// - 返回值已是 [Result]（异常处理器输出即是）
/// - 响应非 JSON 类型（文件下载等）
/// - 响应状态码非 2xx
///
/// 配置项：
///
/// | 配置 | 默认值 | 说明 |
/// |------|--------|------|
/// | `quarkforge.web.response.auto-wrap` | `true` | 是否启用自动包装 |
@ApplicationScoped
public class ResultWrapperFilter {

    @ConfigProperty(name = "quarkforge.web.response.auto-wrap", defaultValue = "true")
    boolean autoWrap;

    @ServerResponseFilter
    public void wrap(ContainerResponseContext responseContext, ResourceInfo resourceInfo) {
        if (!autoWrap || isRaw(resourceInfo)) {
            return;
        }
        int status = responseContext.getStatus();
        // 204/205 按 HTTP 规范不允许携带响应体，跳过包装
        if (status < 200 || status >= 300 || status == 204 || status == 205) {
            return;
        }
        Object entity = responseContext.getEntity();
        if (entity instanceof Result<?> || entity instanceof byte[]) {
            return;
        }
        MediaType mediaType = responseContext.getMediaType();
        if (entity != null && (mediaType == null || !mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE))) {
            return;
        }
        responseContext.setEntity(Result.ok(entity));
        responseContext.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);
    }

    private boolean isRaw(ResourceInfo resourceInfo) {
        if (resourceInfo == null) {
            return false;
        }
        if (resourceInfo.getResourceMethod() != null
                && resourceInfo.getResourceMethod().isAnnotationPresent(RawResponse.class)) {
            return true;
        }
        return resourceInfo.getResourceClass() != null
                && resourceInfo.getResourceClass().isAnnotationPresent(RawResponse.class);
    }
}

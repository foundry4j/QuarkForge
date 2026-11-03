package io.github.foundry4j.quarkforge.web.version;

import jakarta.enterprise.context.RequestScoped;

/// 当前请求命中的 API 版本，由 [ApiVersionFilter] 在请求进入时写入。
///
/// ```java
/// @Inject ApiVersionContext versionContext;
///
/// String version = versionContext.requested(); // "v1"
/// ```
@RequestScoped
public class ApiVersionContext {

    private String requested;

    /// 当前请求指定的版本，已规范化为 `vN` 形式。
    public String requested() {
        return requested;
    }

    void setRequested(String requested) {
        this.requested = requested;
    }
}

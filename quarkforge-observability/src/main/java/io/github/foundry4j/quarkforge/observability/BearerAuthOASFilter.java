package io.github.foundry4j.quarkforge.observability;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/// ## BearerAuthOASFilter
/// 向 OpenAPI 文档注入 `bearerAuth`（JWT）安全方案，
/// 使 Swagger UI 可以直接携带 Bearer Token 调试受保护接口。
@ApplicationScoped
public class BearerAuthOASFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme("bearerAuth", OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"));
    }
}

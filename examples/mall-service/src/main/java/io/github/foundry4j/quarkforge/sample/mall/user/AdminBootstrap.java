package io.github.foundry4j.quarkforge.sample.mall.user;

import io.github.foundry4j.quarkforge.sample.mall.product.ProductService;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/// ## AdminBootstrap
/// 首次启动时创建演示管理员与演示商品，已存在则跳过。
@ApplicationScoped
public class AdminBootstrap {

    @Inject
    UserService userService;

    @Inject
    ProductService productService;

    void onStart(@Observes StartupEvent event) {
        userService.ensureAdmin();
        productService.ensureSampleProducts();
    }
}

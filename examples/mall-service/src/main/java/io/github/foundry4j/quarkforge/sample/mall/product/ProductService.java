package io.github.foundry4j.quarkforge.sample.mall.product;

import io.github.foundry4j.quarkforge.audit.OperationLog;
import io.github.foundry4j.quarkforge.cache.annotation.CacheEvict;
import io.github.foundry4j.quarkforge.cache.annotation.Cacheable;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.core.util.Checks;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.CreateProductRequest;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.ProductResponse;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.UpdateProductRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

/// ## ProductService
/// 商品领域服务：商品管理、详情缓存与库存扣减/恢复。
@ApplicationScoped
public class ProductService {

    private static final String CACHE_NAMESPACE = "product";

    @Inject
    ProductRepository repository;

    /// 商品详情，命中本地缓存；写操作通过 [CacheEvict] 失效。
    @Cacheable(key = "#p0", namespace = CACHE_NAMESPACE, ttl = "PT5M")
    public ProductResponse requireById(Long id) {
        return ProductResponse.from(repository.requireById(id));
    }

    /// 读取最新商品实体（不走缓存），供下单时校验价格与状态。
    public Product requireEntity(Long id) {
        return repository.requireById(id);
    }

    /// 商品分页检索。
    public PageResult<ProductResponse> search(String keyword, int page, int size) {
        return repository.search(keyword, page, size).map(ProductResponse::from);
    }

    /// 新增商品。
    @Transactional
    @OperationLog(module = "product", action = "create", description = "新增商品")
    public ProductResponse create(CreateProductRequest request) {
        Product product = new Product();
        product.name = Checks.notBlank(request.name(), "name").trim();
        product.description = blankToNull(request.description());
        product.price = request.price();
        product.stock = request.stock();
        repository.persist(product);
        return ProductResponse.from(product);
    }

    /// 按需更新非空字段。
    @Transactional
    @OperationLog(module = "product", action = "update", description = "更新商品")
    @CacheEvict(key = "#p0", namespace = CACHE_NAMESPACE)
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = repository.requireById(id);
        if (request.name() != null && !request.name().isBlank()) {
            product.name = request.name().trim();
        }
        if (request.description() != null) {
            product.description = blankToNull(request.description());
        }
        if (request.price() != null) {
            product.price = request.price();
        }
        if (request.stock() != null) {
            product.stock = request.stock();
        }
        if (request.status() != null && !request.status().isBlank()) {
            product.status = ProductStatus.valueOf(request.status().trim().toUpperCase(Locale.ROOT));
        }
        return ProductResponse.from(product);
    }

    /// 原子扣减库存：库存不足时影响行数为 0，抛出参数错误。
    @Transactional
    @CacheEvict(key = "#p0", namespace = CACHE_NAMESPACE)
    public void deductStock(Long productId, int quantity) {
        int updated = repository.update("stock = stock - ?1 where id = ?2 and stock >= ?1", quantity, productId);
        if (updated == 0) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "商品库存不足");
        }
    }

    /// 取消订单时恢复库存。
    @Transactional
    @CacheEvict(key = "#p0", namespace = CACHE_NAMESPACE)
    public void restoreStock(Long productId, int quantity) {
        repository.update("stock = stock + ?1 where id = ?2", quantity, productId);
    }

    /// 首次启动时预置两个演示商品。
    @Transactional
    public void ensureSampleProducts() {
        if (repository.count() > 0) {
            return;
        }
        persistSample("机械键盘", "87 键热插拔机械键盘", new BigDecimal("399.00"), 100);
        persistSample("无线鼠标", "轻量化双模无线鼠标", new BigDecimal("129.00"), 200);
    }

    private void persistSample(String name, String description, BigDecimal price, int stock) {
        Product product = new Product();
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        repository.persist(product);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

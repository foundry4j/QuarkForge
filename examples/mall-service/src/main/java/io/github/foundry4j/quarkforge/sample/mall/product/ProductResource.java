package io.github.foundry4j.quarkforge.sample.mall.product;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.CreateProductRequest;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.ProductResponse;
import io.github.foundry4j.quarkforge.sample.mall.product.dto.UpdateProductRequest;
import io.github.foundry4j.quarkforge.security.RequiresPermission;
import io.github.foundry4j.quarkforge.web.api.PageQuery;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## ProductResource
/// 商品接口。浏览公开，写操作需要 `product:write` 权限。
@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "商品")
public class ProductResource {

    @Inject
    ProductService productService;

    /// 商品分页检索。
    @GET
    public PageResult<ProductResponse> list(@QueryParam("keyword") String keyword,
                                            @BeanParam PageQuery pageQuery) {
        return productService.search(keyword, pageQuery.page(), pageQuery.size());
    }

    /// 商品详情，走本地缓存。
    @GET
    @Path("/{id}")
    public ProductResponse get(@PathParam("id") Long id) {
        return productService.requireById(id);
    }

    /// 新增商品。
    @POST
    @RequiresPermission("product:write")
    public ProductResponse create(@Valid CreateProductRequest request) {
        return productService.create(request);
    }

    /// 更新商品。
    @PUT
    @Path("/{id}")
    @RequiresPermission("product:write")
    public ProductResponse update(@PathParam("id") Long id, @Valid UpdateProductRequest request) {
        return productService.update(id, request);
    }
}

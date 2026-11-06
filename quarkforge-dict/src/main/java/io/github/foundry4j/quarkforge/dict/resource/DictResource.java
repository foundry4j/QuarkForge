package io.github.foundry4j.quarkforge.dict.resource;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.github.foundry4j.quarkforge.dict.dto.DictItemRequest;
import io.github.foundry4j.quarkforge.dict.dto.DictItemView;
import io.github.foundry4j.quarkforge.dict.dto.DictTypeRequest;
import io.github.foundry4j.quarkforge.dict.entity.DictItem;
import io.github.foundry4j.quarkforge.dict.entity.DictType;
import io.github.foundry4j.quarkforge.dict.mapper.DictMapper;
import io.github.foundry4j.quarkforge.dict.service.DictService;
import io.github.foundry4j.quarkforge.web.api.PageQuery;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.util.List;
import java.util.Map;

/// ## DictResource
/// 字典与元数据 REST API：
///
/// | Method | Path | Description |
/// |--------|------|-------------|
/// | GET | `/dict/types` | 分页查询字典类型 |
/// | POST | `/dict/types` | 新建字典类型 |
/// | PUT | `/dict/types/{id}` | 更新字典类型（编码不可改） |
/// | DELETE | `/dict/types/{id}` | 删除字典类型及其字典项 |
/// | GET | `/dict/{typeCode}/items` | 字典项列表，默认仅启用项 |
/// | POST | `/dict/{typeCode}/items` | 新增字典项 |
/// | PUT | `/dict/items/{id}` | 更新字典项 |
/// | DELETE | `/dict/items/{id}` | 删除字典项 |
/// | GET | `/dict/{typeCode}/translate` | 值 → 展示名称映射 |
@Path("/dict")
public class DictResource {

    @Inject
    DictService dictService;

    @Inject
    DictMapper mapper;

    // ---------- 字典类型 ----------

    @GET
    @Path("/types")
    public Result<PageResult<DictType>> searchTypes(
            @QueryParam("keyword") String keyword,
            @QueryParam("enabled") Boolean enabled,
            @BeanParam PageQuery pageQuery) {
        return Result.ok(dictService.searchTypes(keyword, enabled, pageQuery.page(), pageQuery.size()));
    }

    @POST
    @Path("/types")
    public Result<DictType> createType(@Valid DictTypeRequest request) {
        return Result.ok(dictService.createType(request));
    }

    @PUT
    @Path("/types/{id}")
    public Result<DictType> updateType(@PathParam("id") long id, @Valid DictTypeRequest request) {
        return Result.ok(dictService.updateType(id, request));
    }

    @DELETE
    @Path("/types/{id}")
    public Result<Void> deleteType(@PathParam("id") long id) {
        dictService.deleteType(id);
        return Result.ok();
    }

    // ---------- 字典项 ----------

    @GET
    @Path("/{typeCode}/items")
    public Result<List<DictItemView>> items(
            @PathParam("typeCode") String typeCode,
            @QueryParam("enabled") @DefaultValue("true") boolean onlyEnabled) {
        return Result.ok(dictService.items(typeCode, onlyEnabled));
    }

    @POST
    @Path("/{typeCode}/items")
    public Result<DictItemView> createItem(@PathParam("typeCode") String typeCode,
                                           @Valid DictItemRequest request) {
        DictItem item = dictService.createItem(typeCode, request);
        return Result.ok(mapper.toView(item));
    }

    @PUT
    @Path("/items/{id}")
    public Result<DictItemView> updateItem(@PathParam("id") long id, @Valid DictItemRequest request) {
        DictItem item = dictService.updateItem(id, request);
        return Result.ok(mapper.toView(item));
    }

    @DELETE
    @Path("/items/{id}")
    public Result<Void> deleteItem(@PathParam("id") long id) {
        dictService.deleteItem(id);
        return Result.ok();
    }

    @GET
    @Path("/{typeCode}/translate")
    public Result<Map<String, String>> translate(@PathParam("typeCode") String typeCode) {
        return Result.ok(dictService.translate(typeCode));
    }
}

package io.github.foundry4j.quarkforge.dict.service;

import io.github.foundry4j.quarkforge.cache.CacheService;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.core.util.Checks;
import io.github.foundry4j.quarkforge.data.TenantContext;
import io.github.foundry4j.quarkforge.dict.dto.DictItemRequest;
import io.github.foundry4j.quarkforge.dict.dto.DictItemView;
import io.github.foundry4j.quarkforge.dict.dto.DictTypeRequest;
import io.github.foundry4j.quarkforge.dict.entity.DictItem;
import io.github.foundry4j.quarkforge.dict.entity.DictItemRepository;
import io.github.foundry4j.quarkforge.dict.entity.DictType;
import io.github.foundry4j.quarkforge.dict.entity.DictTypeRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// ## DictService
/// 字典与元数据服务：
///
/// - 查询走 [CacheService] 缓存（默认 Caffeine，本地 10 分钟；引入 `quarkforge-redis` 后自动升级为两级缓存）
/// - 任何字典项写入都会立即失效对应类型的缓存，保证读写一致
/// - 缓存键包含租户，多租户数据互不干扰
///
/// ```java
/// List<DictItemView> statuses = dictService.items("order_status");
/// String label = dictService.label("gender", "M");          // "男"
/// ```
@ApplicationScoped
public class DictService {

    private static final String CACHE_PREFIX = "qf:dict:";

    @Inject
    DictTypeRepository typeRepository;

    @Inject
    DictItemRepository itemRepository;

    @Inject
    CacheService cache;

    @Inject
    @ConfigProperty(name = "quarkforge.dict.cache-ttl", defaultValue = "PT10M")
    Duration cacheTtl;

    // ---------- 读取 ----------

    /// 启用的字典项列表（按排序值升序）。
    public List<DictItemView> items(String typeCode) {
        return items(typeCode, true);
    }

    /// 字典项列表；`onlyEnabled` 为 `false` 时包含停用项（历史数据翻译场景使用）。
    public List<DictItemView> items(String typeCode, boolean onlyEnabled) {
        String code = normalizeCode(typeCode);
        DictItemView[] cached = cache.getOrLoad(cacheKey(code, onlyEnabled), DictItemView[].class, cacheTtl,
                () -> loadItems(code, onlyEnabled));
        return List.of(cached);
    }

    /// 值 → 展示名称，值不存在时返回 `null`。
    ///
    /// 使用包含停用项的完整字典，保证历史数据即使字典项已停用也能正确展示。
    public String label(String typeCode, String value) {
        if (value == null) {
            return null;
        }
        return items(typeCode, false).stream()
                .filter(item -> item.value().equals(value))
                .map(DictItemView::label)
                .findFirst()
                .orElse(null);
    }

    /// 整份字典的「值 → 展示名称」映射，保持排序顺序。
    public Map<String, String> translate(String typeCode) {
        Map<String, String> mapping = new LinkedHashMap<>();
        items(typeCode, false).forEach(item -> mapping.putIfAbsent(item.value(), item.label()));
        return mapping;
    }

    /// 分页查询字典类型。
    public PageResult<DictType> searchTypes(String keyword, Boolean enabled, int page, int size) {
        return typeRepository.search(keyword, enabled, page, size);
    }

    // ---------- 字典类型维护 ----------

    /// 新建字典类型。
    @Transactional
    public DictType createType(DictTypeRequest request) {
        String code = normalizeCode(request.code());
        String name = Checks.notBlank(request.name(), "name");
        if (typeRepository.findByCode(code).isPresent()) {
            throw new BizException(CommonErrorCode.CONFLICT, "字典类型已存在: " + code);
        }

        DictType type = new DictType();
        type.code = code;
        type.name = name.trim();
        type.description = request.description();
        type.enabled = request.enabled() == null || request.enabled();
        typeRepository.persist(type);
        return type;
    }

    /// 更新字典类型；编码为业务主键，不允许修改。
    @Transactional
    public DictType updateType(long id, DictTypeRequest request) {
        DictType type = typeRepository.requireById(id);
        String code = normalizeCode(request.code());
        if (!type.code.equals(code)) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "字典编码不可修改: " + type.code);
        }

        type.name = Checks.notBlank(request.name(), "name").trim();
        type.description = request.description();
        if (request.enabled() != null) {
            type.enabled = request.enabled();
        }
        evict(code);
        return type;
    }

    /// 删除字典类型，其下字典项一并删除。
    @Transactional
    public void deleteType(long id) {
        DictType type = typeRepository.requireById(id);
        itemRepository.deleteByType(type.code);
        typeRepository.delete(type);
        evict(type.code);
    }

    // ---------- 字典项维护 ----------

    /// 新增字典项。
    @Transactional
    public DictItem createItem(String typeCode, DictItemRequest request) {
        String code = normalizeCode(typeCode);
        typeRepository.requireByCode(code);

        String value = Checks.notBlank(request.value(), "value").trim();
        if (itemRepository.countByValue(code, value, null) > 0) {
            throw new BizException(CommonErrorCode.CONFLICT, "字典项已存在: " + code + "/" + value);
        }

        DictItem item = new DictItem();
        item.typeCode = code;
        applyRequest(item, request);
        itemRepository.persist(item);
        evict(code);
        return item;
    }

    /// 更新字典项；所属字典类型不可变更。
    @Transactional
    public DictItem updateItem(long id, DictItemRequest request) {
        DictItem item = itemRepository.requireById(id);
        String value = Checks.notBlank(request.value(), "value").trim();
        if (itemRepository.countByValue(item.typeCode, value, id) > 0) {
            throw new BizException(CommonErrorCode.CONFLICT, "字典项已存在: " + item.typeCode + "/" + value);
        }

        applyRequest(item, request);
        evict(item.typeCode);
        return item;
    }

    /// 删除字典项。
    @Transactional
    public void deleteItem(long id) {
        DictItem item = itemRepository.requireById(id);
        itemRepository.delete(item);
        evict(item.typeCode);
    }

    // ---------- 私有 ----------

    private DictItemView[] loadItems(String code, boolean onlyEnabled) {
        typeRepository.requireByCode(code);
        return itemRepository.listByType(code, onlyEnabled).stream()
                .map(DictItemView::from)
                .toArray(DictItemView[]::new);
    }

    private static void applyRequest(DictItem item, DictItemRequest request) {
        item.label = Checks.notBlank(request.label(), "label").trim();
        item.itemValue = request.value().trim();
        item.sortOrder = request.sort() == null ? 0 : request.sort();
        item.enabled = request.enabled() == null || request.enabled();
        item.parentValue = request.parentValue();
        item.remark = request.remark();
    }

    private static String normalizeCode(String code) {
        return Checks.notBlank(code, "code").trim();
    }

    private static String cacheKey(String typeCode, boolean onlyEnabled) {
        return CACHE_PREFIX + TenantContext.current() + ":" + typeCode + ":" + (onlyEnabled ? "enabled" : "all");
    }

    /// 失效某字典类型的两种视图缓存（启用 / 全部）。
    private void evict(String typeCode) {
        cache.evict(cacheKey(typeCode, true));
        cache.evict(cacheKey(typeCode, false));
    }
}

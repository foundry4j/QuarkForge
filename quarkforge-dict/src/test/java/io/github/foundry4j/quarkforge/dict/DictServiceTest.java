package io.github.foundry4j.quarkforge.dict;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.data.TenantContext;
import io.github.foundry4j.quarkforge.dict.dto.DictItemRequest;
import io.github.foundry4j.quarkforge.dict.dto.DictItemView;
import io.github.foundry4j.quarkforge.dict.dto.DictTypeRequest;
import io.github.foundry4j.quarkforge.dict.entity.DictItem;
import io.github.foundry4j.quarkforge.dict.entity.DictItemRepository;
import io.github.foundry4j.quarkforge.dict.entity.DictType;
import io.github.foundry4j.quarkforge.dict.entity.DictTypeRepository;
import io.github.foundry4j.quarkforge.dict.service.DictService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [DictService] 的端到端测试：类型/字典项维护、排序、翻译、缓存失效与多租户隔离。
@QuarkusTest
class DictServiceTest {

    @Inject
    DictService service;

    @Inject
    DictTypeRepository typeRepository;

    @Inject
    DictItemRepository itemRepository;

    @BeforeEach
    @Transactional
    void tearDown() {
        itemRepository.deleteAll();
        typeRepository.deleteAll();
    }

    // ---------- 新建与查询 ----------

    @Test
    @Transactional
    void createsTypeAndListsItemsSorted() {
        service.createType(type("product_unit", "计量单位"));
        service.createItem("product_unit", item("box", "箱", 3, null));
        service.createItem("product_unit", item("piece", "件", 1, null));
        service.createItem("product_unit", item("kg", "千克", 2, null));

        List<DictItemView> items = service.items("product_unit");
        assertEquals(3, items.size());
        assertEquals(List.of("piece", "kg", "box"), items.stream().map(DictItemView::value).toList());
        assertEquals("件", items.getFirst().label());
    }

    @Test
    @Transactional
    void rejectsDuplicateTypeCode() {
        service.createType(type("dup_type", "重复类型"));
        assertThrows(BizException.class, () -> service.createType(type("dup_type", "另一个类型")));
    }

    @Test
    @Transactional
    void rejectsDuplicateItemValue() {
        service.createType(type("dup_item", "重复项"));
        service.createItem("dup_item", item("A", "选项A", 0, null));

        assertThrows(BizException.class, () -> service.createItem("dup_item", item("A", "选项A2", 0, null)));
    }

    @Test
    @Transactional
    void requiresExistingTypeWhenReadingItems() {
        assertThrows(BizException.class, () -> service.items("missing_dict"));
    }

    // ---------- 翻译 ----------

    @Test
    @Transactional
    void translatesValueToLabel() {
        service.createType(type("gender", "性别"));
        service.createItem("gender", item("M", "男", 0, null));
        service.createItem("gender", item("F", "女", 1, null));

        assertEquals("男", service.label("gender", "M"));
        assertNull(service.label("gender", "X"));

        Map<String, String> mapping = service.translate("gender");
        assertEquals(2, mapping.size());
        assertEquals("男", mapping.get("M"));
        assertEquals("女", mapping.get("F"));
    }

    @Test
    @Transactional
    void filtersDisabledItemsButStillTranslatesThem() {
        service.createType(type("status", "状态"));
        service.createItem("status", item("ON", "启用", 0, null));
        service.createItem("status", item("OFF", "停用", 1, null, false));

        assertEquals(1, service.items("status").size());
        assertEquals(2, service.items("status", false).size());
        // 停用项依然能翻译历史数据
        assertEquals("停用", service.label("status", "OFF"));
    }

    // ---------- 缓存 ----------

    @Test
    @Transactional
    void cachesItemsUntilServiceWrites() {
        service.createType(type("cached", "缓存验证"));
        service.createItem("cached", item("A", "旧名称", 0, null));
        assertEquals("旧名称", service.items("cached").getFirst().label());

        // 绕过服务直接改库：缓存未失效，读到的是旧值
        DictItem item = itemRepository.findByTypeAndValue("cached", "A").orElseThrow();
        item.label = "直改名称";
        assertEquals("旧名称", service.items("cached").getFirst().label());

        // 经由服务更新：缓存被清除，读到新值
        service.updateItem(item.id, new DictItemRequest("新名称", "A", 0, null, null, null));
        assertEquals("新名称", service.items("cached").getFirst().label());
    }

    // ---------- 删除 / 搜索 / 多租户 ----------

    @Test
    @Transactional
    void deleteTypeRemovesItsItems() {
        service.createType(type("tmp_del", "待删除"));
        service.createItem("tmp_del", item("A", "选项A", 0, null));
        long typeId = typeRepository.findByCode("tmp_del").orElseThrow().id;

        service.deleteType(typeId);

        assertTrue(typeRepository.findByCode("tmp_del").isEmpty());
        assertEquals(0, itemRepository.count("typeCode = ?1", "tmp_del"));
        assertThrows(BizException.class, () -> service.items("tmp_del"));
    }

    @Test
    @Transactional
    void searchesTypesByKeywordAndEnabled() {
        service.createType(type("order_status", "订单状态"));
        service.createType(new DictTypeRequest("payment_type", "支付方式", "停用的字典", false));

        PageResult<DictType> byKeyword = service.searchTypes("status", null, 1, 10);
        assertEquals(1, byKeyword.total());
        assertEquals("order_status", byKeyword.records().getFirst().code);

        PageResult<DictType> onlyEnabled = service.searchTypes(null, true, 1, 10);
        assertEquals(1, onlyEnabled.total());
        assertEquals("order_status", onlyEnabled.records().getFirst().code);

        PageResult<DictType> all = service.searchTypes(null, null, 1, 10);
        assertEquals(2, all.total());
    }

    @Test
    @Transactional
    void isolatesTenantDictionaries() {
        TenantContext.runAs("acme", () -> {
            service.createType(type("tenant_dict", "租户字典"));
            service.createItem("tenant_dict", item("A", "选项A", 0, null));
        });

        assertEquals("选项A", TenantContext.callAs("acme", () -> service.label("tenant_dict", "A")));

        // 默认租户看不到 acme 的数据
        assertThrows(BizException.class, () -> service.items("tenant_dict"));
        assertTrue(TenantContext.callAs("acme", () -> service.translate("tenant_dict")).containsKey("A"));
    }

    // ---------- 构造工具 ----------

    private static DictTypeRequest type(String code, String name) {
        return new DictTypeRequest(code, name, null, null);
    }

    private static DictItemRequest item(String value, String label, int sort, String parent) {
        return new DictItemRequest(label, value, sort, null, parent, null);
    }

    private static DictItemRequest item(String value, String label, int sort, String parent, boolean enabled) {
        return new DictItemRequest(label, value, sort, enabled, parent, null);
    }
}

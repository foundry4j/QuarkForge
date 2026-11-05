package io.github.foundry4j.quarkforge.dict.entity;

import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/// ## DictItemRepository
/// 字典项仓储。
@ApplicationScoped
public class DictItemRepository extends BaseRepository<DictItem> {

    /// 某字典类型下的全部字典项，按排序值升序；`onlyEnabled` 为 `true` 时过滤停用项。
    public List<DictItem> listByType(String typeCode, boolean onlyEnabled) {
        String query = onlyEnabled
                ? "typeCode = ?1 and enabled = true order by sortOrder asc, id asc"
                : "typeCode = ?1 order by sortOrder asc, id asc";
        return list(query, typeCode);
    }

    /// 按类型 + 值精确查询。
    public Optional<DictItem> findByTypeAndValue(String typeCode, String itemValue) {
        return find("typeCode = ?1 and itemValue = ?2", typeCode, itemValue).singleResultOptional();
    }

    /// 某类型下已存在多少个同值字典项，`excludeId` 用于更新时排除自身。
    public long countByValue(String typeCode, String itemValue, Long excludeId) {
        if (excludeId == null) {
            return count("typeCode = ?1 and itemValue = ?2", typeCode, itemValue);
        }
        return count("typeCode = ?1 and itemValue = ?2 and id <> ?3", typeCode, itemValue, excludeId);
    }

    /// 删除某字典类型下的全部字典项，返回删除条数。
    ///
    /// 使用对象级删除而非 bulk delete：保证 Hibernate 一级缓存与数据库一致。
    public int deleteByType(String typeCode) {
        List<DictItem> items = list("typeCode = ?1", typeCode);
        for (DictItem item : items) {
            delete(item);
        }
        return items.size();
    }
}

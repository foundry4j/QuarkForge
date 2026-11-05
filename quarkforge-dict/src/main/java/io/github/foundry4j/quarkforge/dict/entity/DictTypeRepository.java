package io.github.foundry4j.quarkforge.dict.entity;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// ## DictTypeRepository
/// 字典类型仓储。
@ApplicationScoped
public class DictTypeRepository extends BaseRepository<DictType> {

    /// 按编码查询（租户过滤由 [io.github.foundry4j.quarkforge.data.TenantEntity] 自动追加）。
    public Optional<DictType> findByCode(String code) {
        return find("code = ?1", code).singleResultOptional();
    }

    /// 按编码查询，不存在时抛出 `NOT_FOUND`。
    public DictType requireByCode(String code) {
        return findByCode(code)
                .orElseThrow(() -> new BizException(CommonErrorCode.NOT_FOUND, "字典类型不存在: " + code));
    }

    /// 关键字（编码 / 名称）与启用状态组合筛选，结果按编码升序。
    public PageResult<DictType> search(String keyword, Boolean enabled, int page, int size) {
        StringBuilder query = new StringBuilder("1 = 1");
        List<Object> params = new ArrayList<>();

        if (enabled != null) {
            params.add(enabled);
            query.append(" and enabled = ?").append(params.size());
        }
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            params.add(like);
            int first = params.size();
            params.add(like);
            query.append(" and (lower(code) like ?").append(first)
                    .append(" or lower(name) like ?").append(first + 1).append(')');
        }
        query.append(" order by code");

        return page(find(query.toString(), params.toArray()), page, size);
    }
}

package io.github.foundry4j.quarkforge.sample.user;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.data.BaseRepository;

/// ## UserRepository
/// 用户仓储。继承 [BaseRepository] 后自带分页与 `requireById` 能力。
@ApplicationScoped
public class UserRepository extends BaseRepository<User> {

    /// 按登录名精确查询。
    public Optional<User> findByUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    /// 登录名是否已存在。
    public boolean existsByUsername(String username) {
        return count("username", username) > 0;
    }

    /// 按关键字模糊检索登录名或展示名，关键字为空时返回全量分页。
    public PageResult<User> search(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return pageAll(page, size);
        }
        return page(find("username like ?1 or displayName like ?1", "%" + keyword.trim() + "%"), page, size);
    }
}

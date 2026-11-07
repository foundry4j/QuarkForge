package io.github.foundry4j.quarkforge.sample.mall.user;

import io.github.foundry4j.quarkforge.data.BaseRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/// ## UserRepository
/// 用户仓储。
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
}

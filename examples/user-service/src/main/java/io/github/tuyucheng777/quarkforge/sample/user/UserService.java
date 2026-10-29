package io.github.tuyucheng777.quarkforge.sample.user;

import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.tuyucheng777.quarkforge.core.api.CommonErrorCode;
import io.github.tuyucheng777.quarkforge.core.api.PageResult;
import io.github.tuyucheng777.quarkforge.core.exception.BizException;
import io.github.tuyucheng777.quarkforge.core.util.Checks;
import io.github.tuyucheng777.quarkforge.sample.user.dto.CreateUserRequest;
import io.github.tuyucheng777.quarkforge.sample.user.dto.LoginRequest;
import io.github.tuyucheng777.quarkforge.sample.user.dto.LoginResponse;
import io.github.tuyucheng777.quarkforge.sample.user.dto.UpdateUserRequest;
import io.github.tuyucheng777.quarkforge.security.JwtTokenService;

/// ## UserService
/// 用户领域服务：注册、登录、查询、更新、删除。
/// 所有写操作在事务内完成，读操作直接走仓储。
@ApplicationScoped
public class UserService {

    private static final String DEFAULT_ROLE = "user";

    @Inject
    UserRepository repository;

    @Inject
    JwtTokenService jwtTokenService;

    @ConfigProperty(name = "quarkforge.jwt.ttl-minutes", defaultValue = "120")
    long ttlMinutes;

    /// 注册用户。登录名重复时抛出参数错误。
    @Transactional
    public User create(CreateUserRequest request) {
        String username = Checks.notBlank(request.username(), "username").trim();
        if (repository.existsByUsername(username)) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "用户名已存在: " + username);
        }
        User user = new User();
        user.username = username;
        user.displayName = Checks.notBlank(request.displayName(), "displayName").trim();
        user.email = request.email();
        user.role = request.role() == null || request.role().isBlank() ? DEFAULT_ROLE : request.role();
        user.passwordSalt = PasswordHasher.newSalt();
        user.passwordHash = PasswordHasher.hash(request.password(), user.passwordSalt);
        repository.persist(user);
        return user;
    }

    /// 登录并签发 JWT。用户名不存在或密码错误统一返回未认证，避免账号探测。
    public LoginResponse login(LoginRequest request) {
        User user = repository.findByUsername(request.username())
                .orElseThrow(() -> new BizException(CommonErrorCode.UNAUTHORIZED, "用户名或密码错误"));
        if (!PasswordHasher.matches(request.password(), user.passwordSalt, user.passwordHash)) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.status == UserStatus.DISABLED) {
            throw new BizException(CommonErrorCode.FORBIDDEN, "账号已被禁用");
        }
        String token = jwtTokenService.issue(String.valueOf(user.id), user.username, Set.of(user.role));
        return new LoginResponse(token, "Bearer", ttlMinutes * 60);
    }

    /// 按 ID 查询，不存在时抛出 404 业务异常。
    public User requireById(Long id) {
        return repository.requireById(id);
    }

    /// 关键字分页检索。
    public PageResult<User> search(String keyword, int page, int size) {
        return repository.search(keyword, page, size);
    }

    /// 按需更新非空字段。
    @Transactional
    public User update(Long id, UpdateUserRequest request) {
        User user = repository.requireById(id);
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.displayName = request.displayName().trim();
        }
        if (request.email() != null) {
            user.email = request.email().isBlank() ? null : request.email().trim();
        }
        if (request.status() != null) {
            user.status = request.status();
        }
        return user;
    }

    /// 删除用户，不存在时抛出 404 业务异常。
    @Transactional
    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }
    }
}

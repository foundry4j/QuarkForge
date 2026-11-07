package io.github.foundry4j.quarkforge.sample.mall.user;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.core.util.Checks;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.LoginRequest;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.LoginResponse;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.RegisterRequest;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.UserResponse;
import io.github.foundry4j.quarkforge.security.AuthContext;
import io.github.foundry4j.quarkforge.security.JwtTokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

/// ## UserService
/// 用户领域服务：注册、登录、当前用户查询与启动预置管理员。
@ApplicationScoped
public class UserService {

    /// 管理员角色。
    public static final String ROLE_ADMIN = "ADMIN";

    /// 买家角色。
    public static final String ROLE_BUYER = "BUYER";

    /// 示例内置管理员账号。
    public static final String ADMIN_USERNAME = "admin";

    /// 示例内置管理员密码，仅用于本地演示。
    public static final String ADMIN_PASSWORD = "Admin@123456";

    @Inject
    UserRepository repository;

    @Inject
    JwtTokenService jwtTokenService;

    @Inject
    AuthContext authContext;

    @ConfigProperty(name = "quarkforge.jwt.ttl-minutes", defaultValue = "120")
    long ttlMinutes;

    /// 注册买家。登录名重复时抛出参数错误。
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = Checks.notBlank(request.username(), "username").trim();
        if (repository.existsByUsername(username)) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "用户名已存在: " + username);
        }
        User user = new User();
        user.username = username;
        user.displayName = Checks.notBlank(request.displayName(), "displayName").trim();
        user.phone = blankToNull(request.phone());
        user.role = ROLE_BUYER;
        user.passwordSalt = PasswordHasher.newSalt();
        user.passwordHash = PasswordHasher.hash(request.password(), user.passwordSalt);
        repository.persist(user);
        return UserResponse.from(user);
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
        String token = jwtTokenService.issue(String.valueOf(user.id), user.username,
                Set.of(user.role), permissionsOf(user.role));
        return new LoginResponse(token, "Bearer", ttlMinutes * 60);
    }

    /// 按 ID 查询用户，不存在时抛出 404 业务异常。
    public User requireById(Long id) {
        return repository.requireById(id);
    }

    /// 当前登录用户信息。
    public UserResponse me() {
        return UserResponse.from(requireById(Long.parseLong(authContext.userId())));
    }

    /// 首次启动时创建演示管理员，已存在则跳过。
    @Transactional
    public void ensureAdmin() {
        if (repository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        User admin = new User();
        admin.username = ADMIN_USERNAME;
        admin.displayName = "系统管理员";
        admin.role = ROLE_ADMIN;
        admin.passwordSalt = PasswordHasher.newSalt();
        admin.passwordHash = PasswordHasher.hash(ADMIN_PASSWORD, admin.passwordSalt);
        repository.persist(admin);
    }

    /// 角色到权限的映射：登录时展开写入 JWT 的 `perms` claim。
    private static Set<String> permissionsOf(String role) {
        return switch (role) {
            case ROLE_ADMIN -> Set.of("product:write", "order:read", "oplog:read");
            case ROLE_BUYER -> Set.of("order:write");
            default -> Set.of();
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

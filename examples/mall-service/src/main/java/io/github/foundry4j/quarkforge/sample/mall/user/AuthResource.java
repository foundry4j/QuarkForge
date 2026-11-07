package io.github.foundry4j.quarkforge.sample.mall.user;

import io.github.foundry4j.quarkforge.ratelimit.KeyStrategy;
import io.github.foundry4j.quarkforge.ratelimit.RateLimit;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.LoginRequest;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.LoginResponse;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.RegisterRequest;
import io.github.foundry4j.quarkforge.sample.mall.user.dto.UserResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## AuthResource
/// 认证接口。登录接口按客户端 IP 限流，超出额度返回 HTTP 429。
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "认证")
public class AuthResource {

    @Inject
    UserService userService;

    /// 注册买家账号。
    @POST
    @Path("/register")
    public UserResponse register(@Valid RegisterRequest request) {
        return userService.register(request);
    }

    /// 账号密码登录，签发 JWT。
    @POST
    @Path("/login")
    @RateLimit(permits = 100, windowSeconds = 60, key = KeyStrategy.IP)
    public LoginResponse login(@Valid LoginRequest request) {
        return userService.login(request);
    }
}

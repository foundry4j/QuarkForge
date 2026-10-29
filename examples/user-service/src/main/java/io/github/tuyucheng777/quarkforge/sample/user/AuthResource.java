package io.github.tuyucheng777.quarkforge.sample.user;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.github.tuyucheng777.quarkforge.ratelimit.KeyStrategy;
import io.github.tuyucheng777.quarkforge.ratelimit.RateLimit;
import io.github.tuyucheng777.quarkforge.sample.user.dto.LoginRequest;
import io.github.tuyucheng777.quarkforge.sample.user.dto.LoginResponse;

/// ## AuthResource
/// 认证接口。登录接口带限流：同一客户端 60 秒内最多 20 次尝试，超出返回 HTTP 429。
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "认证")
public class AuthResource {

    @Inject
    UserService userService;

    /// 账号密码登录，签发 JWT。
    @POST
    @Path("/login")
    @RateLimit(permits = 20, windowSeconds = 60, key = KeyStrategy.IP)
    public LoginResponse login(@Valid LoginRequest request) {
        return userService.login(request);
    }
}

package io.github.foundry4j.quarkforge.sample.mall.user;

import io.github.foundry4j.quarkforge.sample.mall.user.dto.UserResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## UserResource
/// 当前登录用户接口。
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "用户")
public class UserResource {

    @Inject
    UserService userService;

    /// 当前登录用户信息，需携带 JWT。
    @GET
    @Path("/me")
    @RolesAllowed({UserService.ROLE_BUYER, UserService.ROLE_ADMIN})
    public UserResponse me() {
        return userService.me();
    }
}

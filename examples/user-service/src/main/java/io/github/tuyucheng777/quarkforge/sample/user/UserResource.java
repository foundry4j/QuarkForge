package io.github.tuyucheng777.quarkforge.sample.user;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.github.tuyucheng777.quarkforge.core.api.PageResult;
import io.github.tuyucheng777.quarkforge.sample.user.dto.CreateUserRequest;
import io.github.tuyucheng777.quarkforge.sample.user.dto.UpdateUserRequest;
import io.github.tuyucheng777.quarkforge.sample.user.dto.UserResponse;
import io.github.tuyucheng777.quarkforge.security.AuthContext;
import io.github.tuyucheng777.quarkforge.web.api.PageQuery;

/// ## UserResource
/// 用户接口。方法直接返回业务对象，由框架自动包装为统一响应体：
/// `{"code":200,"message":"success","data":...}`
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "用户")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    AuthContext authContext;

    /// 注册用户。
    @POST
    public UserResponse create(@Valid CreateUserRequest request) {
        return UserResponse.from(userService.create(request));
    }

    /// 当前登录用户信息，需携带 JWT。
    @GET
    @Path("/me")
    @RolesAllowed({"user", "admin"})
    public UserResponse me() {
        return UserResponse.from(userService.requireById(Long.parseLong(authContext.userId())));
    }

    /// 分页检索用户，`page` 从 1 开始，`size` 上限 100。
    @GET
    public PageResult<UserResponse> list(@QueryParam("keyword") String keyword, @BeanParam PageQuery pageQuery) {
        return userService.search(keyword, pageQuery.page(), pageQuery.size()).map(UserResponse::from);
    }

    /// 查询用户详情。
    @GET
    @Path("/{id}")
    public UserResponse get(@PathParam("id") Long id) {
        return UserResponse.from(userService.requireById(id));
    }

    /// 更新用户资料或状态。
    @PUT
    @Path("/{id}")
    public UserResponse update(@PathParam("id") Long id, @Valid UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    /// 删除用户。
    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) {
        userService.delete(id);
    }
}

package io.github.foundry4j.quarkforge.notification.resource;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.api.Result;
import io.github.foundry4j.quarkforge.notification.entity.Notification;
import io.github.foundry4j.quarkforge.notification.service.NotificationService;
import io.github.foundry4j.quarkforge.security.AuthContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

/// ## NotificationResource
/// 当前登录用户的通知 REST API：
///
/// | Method | Path | Description |
/// |--------|------|-------------|
/// | GET | `/notifications` | 分页查询我的通知，支持未读过滤 |
/// | GET | `/notifications/unread-count` | 未读数 |
/// | PATCH | `/notifications/{id}/read` | 标记单条已读 |
/// | PATCH | `/notifications/read-all` | 全部已读 |
/// | DELETE | `/notifications/{id}` | 删除通知 |
@Path("/notifications")
public class NotificationResource {

    @Inject
    NotificationService service;

    @Inject
    AuthContext auth;

    @GET
    public Result<PageResult<Notification>> list(
            @QueryParam("unread-only") @DefaultValue("false") boolean unreadOnly,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return Result.ok(service.listFor(auth.userId(), unreadOnly, page, size));
    }

    @GET
    @Path("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(service.unreadCount(auth.userId()));
    }

    @PATCH
    @Path("/{id}/read")
    public Result<Void> markRead(@PathParam("id") long id) {
        service.markRead(auth.userId(), id);
        return Result.ok();
    }

    @PATCH
    @Path("/read-all")
    public Result<Integer> markAllRead() {
        return Result.ok(service.markAllRead(auth.userId()));
    }

    @DELETE
    @Path("/{id}")
    public Result<Void> delete(@PathParam("id") long id) {
        service.delete(auth.userId(), id);
        return Result.ok();
    }
}

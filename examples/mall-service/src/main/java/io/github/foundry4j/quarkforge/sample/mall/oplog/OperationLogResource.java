package io.github.foundry4j.quarkforge.sample.mall.oplog;

import io.github.foundry4j.quarkforge.audit.OperationLogEntry;
import io.github.foundry4j.quarkforge.audit.OperationLogRepository;
import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.security.RequiresPermission;
import io.github.foundry4j.quarkforge.web.api.PageQuery;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/// ## OperationLogResource
/// 操作日志查询接口，仅管理员（`oplog:read` 权限）可访问。
@Path("/admin/operation-logs")
@Produces(MediaType.APPLICATION_JSON)
@RequiresPermission("oplog:read")
@Tag(name = "操作日志")
public class OperationLogResource {

    @Inject
    OperationLogRepository repository;

    /// 按模块或操作人筛选操作日志，均不传时返回全部。
    @GET
    public PageResult<OperationLogEntry> list(@QueryParam("module") String module,
                                              @QueryParam("actor") String actor,
                                              @BeanParam PageQuery pageQuery) {
        if (module != null && !module.isBlank()) {
            return repository.pageByModule(module.trim(), pageQuery.page(), pageQuery.size());
        }
        if (actor != null && !actor.isBlank()) {
            return repository.pageByActor(actor.trim(), pageQuery.page(), pageQuery.size());
        }
        return repository.pageAll(pageQuery.page(), pageQuery.size());
    }
}

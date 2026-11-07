package io.github.foundry4j.quarkforge.dict.service;

import io.github.foundry4j.quarkforge.cache.annotation.CacheWarmup;
import io.github.foundry4j.quarkforge.data.TenantContext;
import io.github.foundry4j.quarkforge.dict.entity.DictType;
import io.github.foundry4j.quarkforge.dict.entity.DictTypeRepository;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/// ## DictWarmup
/// 字典缓存预热：应用启动后把当前租户下所有**启用的字典类型**的两种视图
/// （仅启用项 / 全部项）一次性加载进 [io.github.foundry4j.quarkforge.cache.CacheService]，
/// 避免首批翻译请求集中回源。
///
/// 依赖 [CacheWarmup] 注解机制（`quarkforge-cache` 提供）：
///
/// ```properties
/// quarkforge.dict.warmup.enabled=true   # 关闭字典预热
/// ```
///
/// 预热只覆盖当前租户（默认为 `quarkforge.data.tenant.default`）；
/// 其他租户的字典仍在首次读取时按需加载。
@Unremovable
@ApplicationScoped
public class DictWarmup {

    private static final Logger LOG = Logger.getLogger(DictWarmup.class);

    @Inject
    DictTypeRepository typeRepository;

    @Inject
    DictService dictService;

    @Inject
    @ConfigProperty(name = "quarkforge.dict.warmup.enabled", defaultValue = "true")
    boolean enabled;

    @CacheWarmup(name = "dict-items", order = 100)
    @Transactional
    public void warmup() {
        if (!enabled) {
            LOG.debug("字典缓存预热已关闭（quarkforge.dict.warmup.enabled=false）");
            return;
        }

        List<DictType> types = typeRepository.list("enabled = true order by code");
        for (DictType type : types) {
            dictService.items(type.code, true);
            dictService.items(type.code, false);
        }
        LOG.infof("字典缓存预热完成：租户=%s，类型=%d", TenantContext.current(), types.size());
    }
}

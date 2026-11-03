package io.github.foundry4j.quarkforge.scheduler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import io.quarkus.arc.DefaultBean;

/// 本地回退的 StorageProvider：当应用没有 DataSource（也没有其他
/// JobRunr 支持的 SQL/NoSQL 客户端）时，自动启用内存存储。
///
/// 一旦引入 H2/Postgres/MySQL 等数据源，JobRunr Quarkus 扩展会通过
/// build-time 步骤注册 SQL 存储 Provider，后者优先级高于本
/// [DefaultBean]，自然切换到数据库存储（多节点互斥）。
public class StorageProviderProducer {

    @Produces
    @DefaultBean
    @ApplicationScoped
    StorageProvider storageProvider(JobMapper jobMapper) {
        InMemoryStorageProvider provider = new InMemoryStorageProvider();
        provider.setJobMapper(jobMapper);
        return provider;
    }
}

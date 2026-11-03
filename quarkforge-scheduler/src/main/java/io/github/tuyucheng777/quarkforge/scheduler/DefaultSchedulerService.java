package io.github.tuyucheng777.quarkforge.scheduler;

import java.time.Duration;
import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;

/// QuarkForge 任务调度默认实现，直接委托 [JobScheduler]。
///
/// 引入 `quarkforge-scheduler` 后，业务只需注入 [SchedulerService]；
/// 本类通过 [DefaultBean] 标记为默认实现。一旦业务直接依赖
/// JobRunr 的存储（SQL 或 Redis），JobRunr Quarkus 扩展会自动
/// 切换到对应的存储 Provider，多个节点通过同一张表互斥执行，
/// 天然成为分布式调度。
@DefaultBean
@ApplicationScoped
@Unremovable
public class DefaultSchedulerService implements SchedulerService {

    @Inject
    JobScheduler jobScheduler;

    @Override
    public void enqueue(JobLambda job) {
        jobScheduler.enqueue(job);
    }

    @Override
    public void scheduleAt(Instant when, JobLambda job) {
        jobScheduler.schedule(when, job);
    }

    @Override
    public void scheduleCron(String cron, JobLambda job) {
        jobScheduler.scheduleRecurrently(cron, job);
    }

    @Override
    public void scheduleCron(String cron, String jobName, JobLambda job) {
        jobScheduler.scheduleRecurrently(jobName, cron, job);
    }

    @Override
    public void scheduleRecurrently(Duration interval, JobLambda job) {
        jobScheduler.scheduleRecurrently(interval, job);
    }
}

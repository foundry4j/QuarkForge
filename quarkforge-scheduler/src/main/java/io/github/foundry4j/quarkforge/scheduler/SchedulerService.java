package io.github.foundry4j.quarkforge.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.jobrunr.jobs.lambdas.JobLambda;

/// QuarkForge 任务调度 SPI。
///
/// 业务代码应只注入本接口：
/// ```java
/// @Inject SchedulerService scheduler;
/// scheduler.scheduleCron("0 0/5 * * * *", () -> refreshCache());
/// ```
///
/// 默认实现基于 JobRunr 提供的 `JobScheduler`；当存在 SQL DataSource 时
/// 使用数据库存储（天然分布式），否则回退到本地文件存储。
///
/// 参数采用 [JobLambda]（一个 `void run() throws Exception` 的函数式接口），
/// 调用方直接写 lambda 字面量即可匹配。
///
/// @see org.jobrunr.scheduling.JobScheduler
public interface SchedulerService {

    /// 立即入队执行一次。
    void enqueue(JobLambda job);

    /// 在指定时间点执行一次。
    void scheduleAt(Instant when, JobLambda job);

    /// 按 cron 表达式周期执行。
    ///
    /// @param cron 标准 6 段 cron，例如 `0 0/5 * * * *`
    void scheduleCron(String cron, JobLambda job);

    /// 按 cron 表达式周期执行，带任务名（Dashboard 中可辨识）。
    void scheduleCron(String cron, String jobName, JobLambda job);

    /// 按固定间隔周期执行（JobRunr 不区分 fixed-delay / fixed-rate，
    /// 统一按给定周期触发）。
    void scheduleRecurrently(Duration interval, JobLambda job);
}

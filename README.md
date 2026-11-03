<p align="left">
  <img src="docs/logo.svg" alt="QuarkForge logo" width="100">
</p>

# QuarkForge

基于 Quarkus 的模块化快速微服务开发框架，内置统一响应、异常处理、数据访问、安全认证、限流、Redis 与可观测性等日常开发基础能力。

<p align="left">
  <img src="https://img.shields.io/badge/JDK-27-orange" alt="JDK 27">
  <img src="https://img.shields.io/badge/Quarkus-3.40-blue" alt="Quarkus 3.40">
  <img src="https://img.shields.io/badge/Maven-3.9+-red" alt="Maven 3.9+">
</p>

## 模块结构

| 模块 | 职责 | 核心内容 |
|------|------|----------|
| `quarkforge-bom` | 版本对齐 | 一次 import 锁定 Quarkus 平台与全部框架模块版本 |
| `quarkforge-core` | 基础设施（纯 JDK，无框架依赖） | `Result` 统一响应体、`ErrorCode` 错误码、`BizException`、`PageResult`、`Checks` 校验、`SnowflakeIdGenerator` |
| `quarkforge-web` | Web 层 | 全局异常映射、响应体自动包装、`@RawResponse` 豁免、访问日志与 traceId、`PageQuery` 分页参数、文件上传下载、API 版本管理 |
| `quarkforge-data` | 数据访问 | `BaseEntity`（雪花 ID + 审计时间戳）、`BaseRepository`（分页直达 `PageResult`）、`TenantEntity` 多租户自动过滤、`SoftDeleteEntity`/`SoftDeleteRepository` 软删除与回收站、`AuditSupport` 审计 SPI |
| `quarkforge-audit` | 审计日志 | 自动记录新增/更新/删除，更新时输出字段级差异（如价格 100 → 120），`AuditLog` 落库、操作人解析 |
| `quarkforge-crypto` | 加密与脱敏 | AES-256-GCM `CryptoService`、`EncryptedStringConverter` 字段加密、`@Sensitive` 接口返回脱敏（`138****0000`） |
| `quarkforge-cache` | 本地缓存 | `CacheService` SPI、Caffeine 默认实现（按条目 TTL、容量上限） |
| `quarkforge-migration` | 数据库迁移 | Flyway starter，应用启动时自动执行 `db/migration` 下的版本化脚本 |
| `quarkforge-security` | 安全认证 | `JwtTokenService` 令牌签发、`AuthContext` 当前用户解析、`LoginUser` |
| `quarkforge-ratelimit` | 限流 | `@RateLimit` 注解、内存固定窗口默认实现、`RateLimiter` SPI |
| `quarkforge-idempotency` | 幂等 | `@Idempotent` 注解、表达式幂等键、结果缓存回放、`IdempotentStore` SPI（内存默认 / Redis 自动接管） |
| `quarkforge-messaging` | 消息发布 | `MessageEnvelope` 消息信封、`MessagePublisher` SPI、`JsonMessagePublisher` 对象直发（本机默认实现） |
| `quarkforge-messaging-rabbitmq` | RabbitMQ | 交换机声明、routing key 投递、持久化消息、可选 publisher confirms |
| `quarkforge-outbox` | 事务发件箱 | 本地消息表、延迟投递、失败指数退避重试、定时补投 |
| `quarkforge-scheduler` | 任务调度 | JobRunr 集成，支持本地内存存储（单机）和 SQL 存储（分布式），内置 Dashboard |
| `quarkforge-redis` | Redis 集成 | `RedisCacheService` 旁路缓存、`RedisTwoLevelCacheService` 本地 + Redis 两级缓存、`RedisLockService` 分布式锁、Redis 限流与幂等后端（自动接管） |
| `quarkforge-observability` | 可观测性 | OpenAPI/Swagger UI（内置 bearerAuth 方案）、`/q/health` 健康检查、`/q/metrics` Prometheus 指标 |

模块按需引入，互不强绑；`core` 不依赖任何 Quarkus 扩展，可单独用于非 Web 场景。

## 快速开始

新建服务时引入 BOM 与所需模块：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.foundry4j</groupId>
            <artifactId>quarkforge-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.foundry4j</groupId>
        <artifactId>quarkforge-web</artifactId>
    </dependency>
</dependencies>
```

此后资源类直接返回业务对象即可：

```java
@Path("/users")
public class UserResource {

    @GET
    public Map<String, Object> hello() {
        // 自动包装为 {"code":200,"message":"success","data":{"msg":"hi"}}
        return Map.of("msg", "hi");
    }

    @GET
    @Path("/{id}")
    public User get(@RestPath Long id) {
        // 查不到直接抛异常，全局映射为 404 + 统一响应体
        return userRepository.requireById(id);
    }
}
```

## 各模块用法

### 统一响应与异常（core + web）

- 返回 POJO 自动包装为 `Result` 结构；标注 `@RawResponse` 可跳过包装（文件下载、外部回调等场景）。
- 业务代码抛出 `BizException(CommonErrorCode.NOT_FOUND)` 即得到 `404 {"code":40400,...}` 响应。
- 参数校验（`@NotBlank`、`@Valid`）失败自动汇总为 400 响应。
- 每个请求自动生成 `traceId` 写入日志 MDC 与 `X-Trace-Id` 响应头。

### 文件上传下载（web）

`FileStorage` 是存储 SPI，默认实现落本地磁盘（按 `yyyy/MM/dd` 分目录，文件名重写为 UUID，杜绝路径穿越）。

```java
@Inject FileStorage storage;

// multipart 上传
@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
public StoredFile upload(@RestForm FileUpload file) {
    return FileUploads.store(storage, file);
}

// 流式下载，中文文件名按 RFC 5987 编码，浏览器可正确显示
@GET
@Path("/{id}/content")
public Response download(@RestPath Long id) {
    FileRecord record = repository.requireById(id);
    return FileDownloads.download(storage, record.key(), record.filename(), record.contentType());
}
```

内置防护：超出大小上限抛 `413 {"code":41300}`，MIME 类型不在白名单抛 `415 {"code":41500}`，非法存储键抛 400；写入失败会自动清理半成品文件。

```properties
quarkforge.web.file.base-dir=${java.io.tmpdir}/quarkforge-files
quarkforge.web.file.max-size=10M
quarkforge.web.file.allowed-content-types=image/png,application/pdf   # 留空表示不限制
```

接入对象存储时实现 `FileStorage` 并注册为普通 Bean 即可覆盖默认实现。

### API 版本管理（web）

用 `@ApiVersion` 声明接口支持的版本，调用方通过 `X-API-Version` 请求头（或 URL 首段 `/v1/...`）指定：

```java
@Path("/orders")
@ApiVersion("v1")
public class OrderResource {

    @GET
    public PageResult<OrderVO> list() { ... }

    @GET
    @Path("/legacy")
    @ApiVersion(value = "v1", deprecated = true, sunset = "2027-01-01", successor = "/v2/orders")
    public List<OrderVO> legacyList() { ... }
}
```

- 请求版本不在支持列表内 → `400 {"code":40001,...}`
- 响应固定回写 `X-API-Version`
- 废弃接口额外返回 `Deprecation: true`、`Sunset: 2027-01-01`、`Link: </v2/orders>; rel="successor-version"`
- 未标注 `@ApiVersion` 的接口不做校验，向后兼容；版本号 `1` 与 `v1` 等价

版本号通过过滤器解析后可注入 `ApiVersionContext` 读取：

```java
@Inject ApiVersionContext versionContext;
String version = versionContext.requested();
```

### 数据访问（data）

```java
@Entity
public class User extends BaseEntity {   // 自动获得雪花 id / createdAt / updatedAt
    public String name;
}

@ApplicationScoped
public class UserRepository extends BaseRepository<User> {
    public PageResult<User> search(String keyword, PageQuery query) {
        return page(find("name like ?1", "%" + keyword + "%"), query.page(), query.size());
    }
}
```

多实例部署时通过 `quarkforge.data.worker-id` 区分雪花节点。

### 多租户与软删除（data）

```java
// 继承 TenantEntity：写入自动填充 tenant_id，查询与按 ID 加载自动带上租户条件
@Entity
public class Order extends TenantEntity { }

// 定时任务 / 后台管理跨租户执行
TenantContext.runAs("tenant-a", () -> orderRepository.listAll());

// 继承 SoftDeleteEntity + SoftDeleteRepository
@Entity
public class Article extends SoftDeleteEntity { }

@ApplicationScoped
public class ArticleRepository extends SoftDeleteRepository<Article> { }

article.delete();                              // 只打标记，默认列表不可见
articleRepository.findAllIncludingDeleted();   // 回收站
articleRepository.restoreById(id);             // 恢复
articleRepository.purgeById(id);               // 物理删除
```

未显式设置租户时使用默认租户 `default`（`quarkforge.data.tenant.default`）。请求入口可按项目鉴权体系调用 `TenantContext.set(...)` 透传租户。

### 审计日志（audit）

引入 `quarkforge-audit` 即自动生效，业务代码无需改动：

```java
@Inject AuditLogRepository auditLogRepository;

List<AuditLog> logs = auditLogRepository.findByEntity(Order.class.getName(), orderId);
// action=UPDATE，actor=bob，changes={"changes":[{"field":"price","oldValue":"100","newValue":"120"}]}
```

- 操作人优先取 `ActorContext`，其次当前 `SecurityIdentity`，最后 `system`
- 审计与业务数据在同一事务落库，业务回滚时审计一并回滚
- `@NotAudited` 可跳过指定实体；JPQL / 原生批量更新不触发生命周期回调，因此不会被审计

### 字段加密与脱敏（crypto）

```properties
# 32 字节密钥的 Base64，可用 openssl rand -base64 32 生成
quarkforge.crypto.key=...
```

```java
@Convert(converter = EncryptedStringConverter.class)
@Sensitive(SensitiveType.PHONE)
public String phone;   // 数据库存密文，接口返回 "138****0000"
```

内置 `PHONE` / `ID_CARD` / `EMAIL` / `BANK_CARD` / `NAME` / `PASSWORD` / `DEFAULT` 规则，也可直接调用 `Masking.mask(value, type)`。

### 数据库迁移（migration）

引入 `quarkforge-migration` 后，启动时自动执行版本化脚本：

```text
src/main/resources/db/migration/
├── V1__create_order.sql
└── V2__add_order_note.sql
```

默认 `migrate-at-start=true`、`baseline-on-migrate=true`，可在业务的 `application.properties` 中覆盖。

### 任务调度（scheduler）

基于 JobRunr 9（原生支持 Quarkus 3.40 LTS），本地零配置就能跑，加数据库即自动成为分布式调度。

```java
@Inject SchedulerService scheduler;

// 立即执行（静态方法引用匹配 JobLambda，可被 JobRunr 序列化）
scheduler.enqueue(TicketService::processDailyReports);

// 延迟到指定时间点执行一次
scheduler.scheduleAt(Instant.now().plus(Duration.ofMinutes(30)),
        OrderService::sendClosingReminder);

// 标准 6 段 cron 周期执行
scheduler.scheduleCron("0 0 2 * * *", BillingService::runNightlySettlement);
scheduler.scheduleCron("0 0/5 * * * *", "refresh-metrics", MetricsService::refreshCaches);

// 固定间隔周期执行（JobRunr 不区分 fixed-delay / fixed-rate）
scheduler.scheduleRecurrently(Duration.ofSeconds(30), Heartbeat::send);
```

**本地 vs 分布式的自动切换**：

- **单机模式（默认）**：没有 DataSource 时，[StorageProviderProducer](quarkforge-scheduler/src/main/java/io/github/foundry4j/quarkforge/scheduler/StorageProviderProducer.java) 提供的 `@DefaultBean InMemoryStorageProvider` 生效，BackgroundJobServer 在同一个 JVM 里轮询执行任务。适合开发和单节点部署。

- **分布式模式**：引入 H2 / Postgres / MySQL 任何一个 DataSource 后，JobRunr Quarkus 扩展自动检测并注册 `SqlStorageProvider`，所有节点共享同一张 `jobrunr_*` 表，通过乐观锁互斥执行——**多节点部署时同一任务只会被一个节点执行一次**。这也是 JobRunr 的核心优势，比自建 Redis 锁或 Quartz 集群表更轻量。

**常用配置**（`quarkus.jobrunr.*` 前缀，可在业务 application.properties 覆盖）：

```properties
# 后台任务处理服务器默认开启，可改 poll-interval 调整轮询频率
quarkus.jobrunr.background-job-server.poll-interval=5   # 秒（生产建议 15-30）

# 失败重试与任务保留
quarkus.jobrunr.jobs.default-number-of-retries=3
quarkus.jobrunr.jobs.delete-succeeded-jobs-after=36h
quarkus.jobrunr.jobs.permanently-delete-deleted-jobs-after=72h

# Dashboard 默认关闭，生产需显式开启并加鉴权
quarkus.jobrunr.dashboard.enabled=false
quarkus.jobrunr.dashboard.port=8000
```

> 调用方法引用或 lambda 时请确保目标**可被 JobRunr 序列化**：
> - 静态方法引用（如 `MyService::myStaticMethod`）✅ 最安全，后台 runner 直接反射调用
> - 实例方法引用（如 `myBean::doWork`）需要通过 ArC 找到 CDI bean 实例，确保该 bean 已注册
> - lambda 表达式会捕获所在类的引用，反序列化需要该类在 classpath 内

### 缓存（cache / redis）

```java
@Inject CacheService cache;

// 未引入 Redis 时：Caffeine 本地缓存
cache.getOrLoad("user:" + id, User.class, Duration.ofMinutes(10), () -> load(id));

// 引入 quarkforge-redis 后自动切换为两级缓存：本地 → Redis → loader
```

### 安全认证（security）

```java
@Inject JwtTokenService jwt;
@Inject AuthContext auth;

// 登录接口签发令牌
String token = jwt.issue(user.id.toString(), user.name, Set.of("admin"));

// 业务接口读取当前用户，未登录自动抛出 401
LoginUser current = auth.currentUser();
```

签名/验签密钥沿用 SmallRye JWT 标准配置（推荐 RSA 密钥文件，生产环境请勿提交私钥）：

```properties
smallrye.jwt.sign.key.location=jwt/privateKey.pem
mp.jwt.verify.publickey.location=jwt/publicKey.pem
mp.jwt.verify.issuer=quarkforge
```

### 限流（ratelimit）

```java
@POST
@RateLimit(permits = 5, windowSeconds = 60, key = KeyStrategy.USER)
public Result<Void> sendSmsCode() { ... }
```

支持 `USER` / `IP` / `GLOBAL` 三种维度，超额返回 HTTP 429。默认内存实现适合单机；引入 `quarkforge-redis` 后自动切换为 Redis 分布式限流。

### 幂等（idempotency）

```java
@POST
@Idempotent(key = "order:#req.orderNo", ttlSeconds = 600)
public Result<OrderVO> createOrder(@RequestBody CreateOrderRequest req) {
    // 相同 orderNo 的重复请求：
    // - 首次执行业务方法并缓存返回值 600 秒
    // - 缓存期内直接回放首次结果，不重复执行
    // - 并发同键请求返回 HTTP 409（CONFLICT）
}
```

`key` 支持 `#参数名` 与 `#p0` 索引，并可链式 `.字段`；留空时按参数序列化内容生成键。默认内存存储适合单机，引入 `quarkforge-redis` 后自动切换为分布式实现。业务方法抛异常会立即释放幂等键，允许后续重试。

### 消息（messaging + RabbitMQ）

默认不依赖中间件：消息只写入内存并记录日志（`LocalMessagePublisher`），本地开发可直接跑通；引入 `quarkforge-messaging-rabbitmq` 后自动切换为真实投递。

```properties
quarkforge.messaging.rabbitmq.uri=amqp://guest:guest@localhost:5672/
quarkforge.messaging.rabbitmq.exchange=quarkforge.topic
quarkforge.messaging.rabbitmq.exchange-type=topic
quarkforge.messaging.rabbitmq.publisher-confirms=true
```

```java
@Inject JsonMessagePublisher publisher;

publisher.publish("order.created", Map.of("orderNo", orderNo));
```

`destination` 作为 routing key；`exchange` 留空则投递到默认交换机（此时 `destination` 就是队列名）。消费端可直接使用 Quarkus 原生 `@Incoming` 或 RabbitMQ 客户端。

### 事务发件箱（outbox）

业务写库与消息发送放在同一个事务，避免「订单已落库、消息没发出去」：

```java
@Transactional
public void createOrder(CreateOrderRequest request) {
    orderRepository.persist(order);
    outbox.enqueue("order.created", new OrderCreatedEvent(order.id, order.no));
}
```

延迟投递与失败重试：

```java
// 30 分钟后触发关单检查
outbox.enqueue("order.close-check", Map.of("orderNo", orderNo), Duration.ofMinutes(30));
```

后台每 5 秒扫描本地消息表补投；投递失败按 `30s → 60s → 120s …` 指数退避，超过 5 次标记 `FAILED`。多实例部署时通过条件更新抢占消息，同一条消息只会被一个实例投递。普通定时任务可直接使用 Quarkus `@Scheduled`。

### Redis（redis）

```java
// 旁路缓存
User user = cache.getOrLoad("user:" + id, User.class, Duration.ofMinutes(10),
        () -> userRepository.requireById(id));

// 分布式锁
lock.withLock("order:pay:" + orderId, Duration.ofSeconds(30), () -> doPay());
```

### 可观测性（observability）

引入即用：Swagger UI 位于 `/q/swagger-ui`（已内置 JWT bearerAuth 调试方案），健康检查 `/q/health`，Prometheus 指标 `/q/metrics`。

## 示例服务

`examples/user-service` 是基于本框架的用户微服务示例，串起 web / data / security / ratelimit / observability 各模块：

| 能力 | 示例中的落点 |
|------|--------------|
| 用户 CRUD | [UserResource](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserResource.java) + [UserService](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserService.java) + [UserRepository](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserRepository.java) |
| 统一响应 | 资源方法直接返回 DTO，自动包装为 `Result` 信封 |
| 异常与校验 | 用户名重复抛出 `BizException`，请求体校验失败自动返回 400 |
| 登录鉴权 | `JwtTokenService` 签发 JWT，`/users/me` 用 `@RolesAllowed` 保护 |
| 限流 | 登录接口 `@RateLimit(permits = 20, windowSeconds = 60)` |
| 文档监控 | Swagger UI、`/q/health`、`/q/metrics` |

接口速览：

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/users` | 注册用户 |
| GET | `/users?keyword=&page=&size=` | 关键字分页检索 |
| GET | `/users/{id}` | 用户详情，不存在返回 404 |
| PUT | `/users/{id}` | 更新资料或状态 |
| DELETE | `/users/{id}` | 删除用户（204） |
| POST | `/auth/login` | 登录换取 JWT |
| GET | `/users/me` | 当前登录用户，需 `Authorization: Bearer <token>` |

本地启动：

```bash
mvn -pl examples/user-service -am install -DskipTests
mvn -pl examples/user-service quarkus:dev
```

示例使用内存 H2 与仓库内的演示 JWT 密钥，可直接运行；生产请替换数据源与密钥。

## 配置项

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `quarkforge.web.response.auto-wrap` | `true` | 响应体自动包装开关 |
| `quarkforge.web.access-log.enabled` | `true` | 访问日志开关 |
| `quarkforge.web.file.base-dir` | `${java.io.tmpdir}/quarkforge-files` | 文件存储根目录 |
| `quarkforge.web.file.max-size` | `10M` | 单文件大小上限，超出返回 413 |
| `quarkforge.web.file.allowed-content-types` | 空（不限制） | 允许上传的 MIME 类型，逗号分隔 |
| `quarkforge.web.version.enabled` | `true` | API 版本处理开关 |
| `quarkforge.web.version.default` | `v1` | 未指定版本时使用的默认版本 |
| `quarkforge.data.worker-id` | `0` | 雪花 ID 节点号（[0, 1023]） |
| `quarkforge.data.tenant.default` | `default` | 未显式设置租户时使用的租户 ID |
| `quarkforge.audit.enabled` | `true` | 审计日志落库开关 |
| `quarkforge.crypto.key` | 无（必填） | 字段加密密钥，32 字节的 Base64 |
| `quarkforge.cache.local.max-size` | `10000` | 本地缓存最大条目数 |
| `quarkforge.cache.local.default-ttl` | `PT5M` | 本地缓存默认过期时间 |
| `quarkforge.jwt.issuer` | `quarkforge` | JWT 签发方 |
| `quarkforge.jwt.ttl-minutes` | `120` | JWT 有效期（分钟） |
| `quarkforge.outbox.enabled` | `true` | 发件箱后台投递开关 |
| `quarkforge.outbox.dispatch-interval` | `5s` | 发件箱扫描间隔 |
| `quarkforge.outbox.max-attempts` | `5` | 单条消息最大投递次数 |
| `quarkforge.outbox.backoff-seconds` | `30` | 首次失败退避秒数（之后指数翻倍） |
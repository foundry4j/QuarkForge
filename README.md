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
| `quarkforge-web` | Web 层 | 全局异常映射、响应体自动包装、`@RawResponse` 豁免、访问日志与 traceId、`PageQuery` 分页参数 |
| `quarkforge-data` | 数据访问 | `BaseEntity`（雪花 ID + 审计时间戳）、`BaseRepository`（分页直达 `PageResult`） |
| `quarkforge-security` | 安全认证 | `JwtTokenService` 令牌签发、`AuthContext` 当前用户解析、`LoginUser` |
| `quarkforge-ratelimit` | 限流 | `@RateLimit` 注解、内存固定窗口默认实现、`RateLimiter` SPI |
| `quarkforge-idempotency` | 幂等 | `@Idempotent` 注解、表达式幂等键、结果缓存回放、`IdempotentStore` SPI（内存默认 / Redis 自动接管） |
| `quarkforge-redis` | Redis 集成 | `RedisCacheService` 旁路缓存、`RedisLockService` 分布式锁、Redis 限流与幂等后端（自动接管） |
| `quarkforge-observability` | 可观测性 | OpenAPI/Swagger UI（内置 bearerAuth 方案）、`/q/health` 健康检查、`/q/metrics` Prometheus 指标 |

模块按需引入，互不强绑；`core` 不依赖任何 Quarkus 扩展，可单独用于非 Web 场景。

## 快速开始

新建服务时引入 BOM 与所需模块：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.tuyucheng777</groupId>
            <artifactId>quarkforge-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.tuyucheng777</groupId>
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
| 用户 CRUD | [UserResource](examples/user-service/src/main/java/io/github/tuyucheng777/quarkforge/sample/user/UserResource.java) + [UserService](examples/user-service/src/main/java/io/github/tuyucheng777/quarkforge/sample/user/UserService.java) + [UserRepository](examples/user-service/src/main/java/io/github/tuyucheng777/quarkforge/sample/user/UserRepository.java) |
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
| `quarkforge.data.worker-id` | `0` | 雪花 ID 节点号（[0, 1023]） |
| `quarkforge.jwt.issuer` | `quarkforge` | JWT 签发方 |
| `quarkforge.jwt.ttl-minutes` | `120` | JWT 有效期（分钟） |
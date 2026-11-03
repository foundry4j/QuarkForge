# QuarkForge

<img src="docs/logo.svg" alt="QuarkForge logo" align="right" width="128" height="128" />

<p align="left">
  <img src="https://img.shields.io/badge/JDK-27-orange" alt="JDK 27">
  <img src="https://img.shields.io/badge/Quarkus-3.40-blue" alt="Quarkus 3.40">
  <img src="https://img.shields.io/badge/Maven-3.9+-red" alt="Maven 3.9+">
</p>

A modular rapid microservice development framework based on Quarkus, with built-in everyday development fundamentals such as unified responses, exception handling, data access, security authentication, rate limiting, Redis, and observability.

## Module Structure

| Module | Responsibility | Core Contents |
|--------|----------------|---------------|
| `quarkforge-bom` | Version alignment | Single import that locks the Quarkus platform and all framework module versions |
| `quarkforge-core` | Infrastructure (pure JDK, no framework dependencies) | `Result` unified response body, `ErrorCode` error codes, `BizException`, `PageResult`, `Checks` validation, `SnowflakeIdGenerator` |
| `quarkforge-web` | Web layer | Global exception mapping, automatic response wrapping, `@RawResponse` exemption, access log with traceId, `PageQuery` pagination parameters, file upload/download, API versioning |
| `quarkforge-validation` | Parameter validation | Business-oriented Bean Validation annotations: `@Phone`, `@IdCard`, `@BankCard`, `@PasswordStrength`, `@EnumValid` |
| `quarkforge-data` | Data access | `BaseEntity` (snowflake ID + audit timestamps), `BaseRepository` (pagination straight to `PageResult`), `TenantEntity` multi-tenant auto-filtering, `SoftDeleteEntity`/`SoftDeleteRepository` soft delete with recycle bin, `AuditSupport` audit SPI |
| `quarkforge-audit` | Audit logging | Automatically records create/update/delete operations with field-level diffs on updates (e.g. price 100 → 120), `AuditLog` persistence, operator resolution |
| `quarkforge-crypto` | Encryption & masking | AES-256-GCM `CryptoService`, `EncryptedStringConverter` field encryption, `@Sensitive` response masking (`138****0000`) |
| `quarkforge-cache` | Local cache | `CacheService` SPI, Caffeine default implementation (per-entry TTL, capacity limit) |
| `quarkforge-migration` | Database migration | Flyway starter that automatically runs versioned scripts under `db/migration` at application startup |
| `quarkforge-security` | Security & authentication | `JwtTokenService` token issuance, `AuthContext` current-user resolution, `LoginUser` |
| `quarkforge-ratelimit` | Rate limiting | `@RateLimit` annotation, in-memory fixed-window default implementation, `RateLimiter` SPI |
| `quarkforge-idempotency` | Idempotency | `@Idempotent` annotation, expression-based idempotency keys, cached result replay, `IdempotentStore` SPI (in-memory default / Redis auto-takeover) |
| `quarkforge-messaging` | Message publishing | `MessageEnvelope` message envelope, `MessagePublisher` SPI, `JsonMessagePublisher` direct object publishing (local default implementation) |
| `quarkforge-messaging-rabbitmq` | RabbitMQ | Exchange declaration, routing-key delivery, persistent messages, optional publisher confirms |
| `quarkforge-outbox` | Transactional outbox | Local message table, delayed delivery, exponential-backoff retry on failure, scheduled redelivery |
| `quarkforge-rpc` | RPC communication | gRPC infrastructure: `BizException` ↔ gRPC status mapping with error-code trailers, traceId propagation interceptors, built-in `Echo` diagnostics service |
| `quarkforge-scheduler` | Job scheduling | JobRunr integration with in-memory storage (standalone) and SQL storage (distributed), built-in dashboard |
| `quarkforge-redis` | Redis integration | `RedisCacheService` cache-aside, `RedisTwoLevelCacheService` local + Redis two-level cache, `RedisLockService` distributed locks, Redis backends for rate limiting and idempotency (auto-takeover) |
| `quarkforge-observability` | Observability | OpenAPI/Swagger UI (built-in bearerAuth scheme), `/q/health` health checks, `/q/metrics` Prometheus metrics |

Modules are pulled in on demand with no forced coupling; `core` has no Quarkus extension dependencies and can be used standalone in non-web scenarios.

## Getting Started

Import the BOM and the modules you need in a new service:

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

Resource classes can then return business objects directly:

```java
@Path("/users")
public class UserResource {

    @GET
    public Map<String, Object> hello() {
        // Automatically wrapped as {"code":200,"message":"success","data":{"msg":"hi"}}
        return Map.of("msg", "hi");
    }

    @GET
    @Path("/{id}")
    public User get(@RestPath Long id) {
        // Throw an exception when not found; globally mapped to 404 + unified response body
        return userRepository.requireById(id);
    }
}
```

## Module Usage

### Unified Responses & Exceptions (core + web)

- Returned POJOs are automatically wrapped in the `Result` structure; annotate with `@RawResponse` to skip wrapping (file downloads, external callbacks, etc.).
- Throwing `BizException(CommonErrorCode.NOT_FOUND)` in business code yields a `404 {"code":40400,...}` response.
- Parameter validation failures (`@NotBlank`, `@Valid`) are automatically aggregated into a 400 response.
- Every request gets a `traceId` written to the logging MDC and the `X-Trace-Id` response header.

### Parameter Validation (validation)

Adds business-oriented validation annotations on top of standard Bean Validation (`@NotBlank` / `@Size`, etc.). Validation failures are mapped by the web layer to `400 {"code":40000,"message":"field error-message"}`.

```java
public class RegisterRequest {

    @NotBlank
    public String username;

    @Phone                                // 11 digits starting with 1[3-9]
    public String phone;

    @IdCard                               // 18 digits + birth date + weighted check digit
    public String idCard;

    @BankCard                             // 12-19 digits + Luhn checksum
    public String bankCard;

    @PasswordStrength(min = 8, requireSpecial = true)
    public String password;

    @EnumValid(UserStatus.class)          // Must be an enum constant name; ignoreCase supported
    public String status;
}
```

All annotations follow the convention "`null` is valid; leave required-ness to `@NotNull`". `@Phone`/`@IdCard`/`@BankCard`/`@PasswordStrength` apply to string fields; `@EnumValid` applies to string fields with a target enum specified.

### File Upload & Download (web)

`FileStorage` is the storage SPI; the default implementation writes to the local disk (partitioned by `yyyy/MM/dd`, filenames rewritten as UUIDs, path traversal prevented).

```java
@Inject FileStorage storage;

// Multipart upload
@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
public StoredFile upload(@RestForm FileUpload file) {
    return FileUploads.store(storage, file);
}

// Streaming download; non-ASCII filenames are RFC 5987 encoded so browsers display them correctly
@GET
@Path("/{id}/content")
public Response download(@RestPath Long id) {
    FileRecord record = repository.requireById(id);
    return FileDownloads.download(storage, record.key(), record.filename(), record.contentType());
}
```

Built-in protection: exceeding the size limit throws `413 {"code":41300}`, a MIME type outside the whitelist throws `415 {"code":41500}`, and an illegal storage key throws 400; partially written files are cleaned up automatically on write failure.

```properties
quarkforge.web.file.base-dir=${java.io.tmpdir}/quarkforge-files
quarkforge.web.file.max-size=10M
quarkforge.web.file.allowed-content-types=image/png,application/pdf   # empty means unrestricted
```

To integrate object storage, implement `FileStorage` and register it as a regular bean to override the default.

### API Versioning (web)

Declare supported versions with `@ApiVersion`; callers specify the version via the `X-API-Version` header (or the first URL segment `/v1/...`):

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

- Requested version not in the supported list → `400 {"code":40001,...}`
- Responses always write back `X-API-Version`
- Deprecated endpoints additionally return `Deprecation: true`, `Sunset: 2027-01-01`, `Link: </v2/orders>; rel="successor-version"`
- Endpoints without `@ApiVersion` are not validated, keeping backward compatibility; version numbers `1` and `v1` are equivalent

After the filter resolves the version, it can be read by injecting `ApiVersionContext`:

```java
@Inject ApiVersionContext versionContext;
String version = versionContext.requested();
```

### Data Access (data)

```java
@Entity
public class User extends BaseEntity {   // Automatically gets snowflake id / createdAt / updatedAt
    public String name;
}

@ApplicationScoped
public class UserRepository extends BaseRepository<User> {
    public PageResult<User> search(String keyword, PageQuery query) {
        return page(find("name like ?1", "%" + keyword + "%"), query.page(), query.size());
    }
}
```

In multi-instance deployments, distinguish snowflake nodes via `quarkforge.data.worker-id`.

### Multi-Tenancy & Soft Delete (data)

```java
// Extend TenantEntity: writes auto-fill tenant_id; queries and load-by-id automatically carry the tenant condition
@Entity
public class Order extends TenantEntity { }

// Scheduled jobs / admin tasks can run across tenants
TenantContext.runAs("tenant-a", () -> orderRepository.listAll());

// Extend SoftDeleteEntity + SoftDeleteRepository
@Entity
public class Article extends SoftDeleteEntity { }

@ApplicationScoped
public class ArticleRepository extends SoftDeleteRepository<Article> { }

article.delete();                              // Only sets the flag; hidden from default listings
articleRepository.findAllIncludingDeleted();   // Recycle bin
articleRepository.restoreById(id);             // Restore
articleRepository.purgeById(id);               // Physical delete
```

When no tenant is set explicitly, the default tenant `default` is used (`quarkforge.data.tenant.default`). The request entry point can call `TenantContext.set(...)` to propagate the tenant according to the project's authentication scheme.

### Audit Logging (audit)

Adding `quarkforge-audit` takes effect automatically with no business code changes:

```java
@Inject AuditLogRepository auditLogRepository;

List<AuditLog> logs = auditLogRepository.findByEntity(Order.class.getName(), orderId);
// action=UPDATE, actor=bob, changes={"changes":[{"field":"price","oldValue":"100","newValue":"120"}]}
```

- The operator is resolved from `ActorContext` first, then the current `SecurityIdentity`, and finally `system`
- Audit records are persisted in the same transaction as business data; both roll back together
- `@NotAudited` skips specified entities; JPQL / native bulk updates do not trigger lifecycle callbacks and are therefore not audited

### Field Encryption & Masking (crypto)

```properties
# Base64 of a 32-byte key; generate with openssl rand -base64 32
quarkforge.crypto.key=...
```

```java
@Convert(converter = EncryptedStringConverter.class)
@Sensitive(SensitiveType.PHONE)
public String phone;   // Ciphertext in the database, "138****0000" in API responses
```

Built-in rules: `PHONE` / `ID_CARD` / `EMAIL` / `BANK_CARD` / `NAME` / `PASSWORD` / `DEFAULT`; you can also call `Masking.mask(value, type)` directly.

### Database Migration (migration)

After adding `quarkforge-migration`, versioned scripts run automatically at startup:

```text
src/main/resources/db/migration/
├── V1__create_order.sql
└── V2__add_order_note.sql
```

Defaults are `migrate-at-start=true` and `baseline-on-migrate=true`; override them in the application's `application.properties`.

### Job Scheduling (scheduler)

Built on JobRunr 9 (native support for Quarkus 3.40 LTS). It runs locally with zero configuration, and adding a database automatically turns it into a distributed scheduler.

```java
@Inject SchedulerService scheduler;

// Fire-and-forget (a static method reference matches JobLambda and is serializable by JobRunr)
scheduler.enqueue(TicketService::processDailyReports);

// Run once at a specific instant
scheduler.scheduleAt(Instant.now().plus(Duration.ofMinutes(30)),
        OrderService::sendClosingReminder);

// Standard 6-field cron schedule
scheduler.scheduleCron("0 0 2 * * *", BillingService::runNightlySettlement);
scheduler.scheduleCron("0 0/5 * * * *", "refresh-metrics", MetricsService::refreshCaches);

// Fixed-interval recurring job (JobRunr does not distinguish fixed-delay / fixed-rate)
scheduler.scheduleRecurrently(Duration.ofSeconds(30), Heartbeat::send);
```

**Automatic switching between local and distributed modes**:

- **Standalone mode (default)**: without a DataSource, the `@DefaultBean InMemoryStorageProvider` provided by [StorageProviderProducer](quarkforge-scheduler/src/main/java/io/github/foundry4j/quarkforge/scheduler/StorageProviderProducer.java) takes effect, and the BackgroundJobServer polls and executes jobs within the same JVM. Suitable for development and single-node deployments.

- **Distributed mode**: once any DataSource (H2 / Postgres / MySQL) is present, the JobRunr Quarkus extension automatically detects it and registers a `SqlStorageProvider`. All nodes share the same `jobrunr_*` tables and use optimistic locking for mutual exclusion — **in a multi-node deployment each job is executed by exactly one node**. This is JobRunr's core advantage: lighter than a hand-rolled Redis lock or a Quartz cluster table.

**Common configuration** (`quarkus.jobrunr.*` prefix; override in the application's application.properties):

```properties
# The background job server is enabled by default; tune the poll interval as needed
quarkus.jobrunr.background-job-server.poll-interval=5   # seconds (15-30 recommended in production)

# Retry and job retention
quarkus.jobrunr.jobs.default-number-of-retries=3
quarkus.jobrunr.jobs.delete-succeeded-jobs-after=36h
quarkus.jobrunr.jobs.permanently-delete-deleted-jobs-after=72h

# The dashboard is disabled by default; enable it explicitly and secure it in production
quarkus.jobrunr.dashboard.enabled=false
quarkus.jobrunr.dashboard.port=8000
```

> When passing method references or lambdas, make sure the target **can be serialized by JobRunr**:
> - Static method references (e.g. `MyService::myStaticMethod`) ✅ safest — the background runner invokes them directly via reflection
> - Instance method references (e.g. `myBean::doWork`) require the CDI bean instance to be resolvable through ArC, so make sure the bean is registered
> - Lambda expressions capture the enclosing class reference, so deserialization requires that class to be on the classpath

### Caching (cache / redis)

```java
@Inject CacheService cache;

// Without Redis: Caffeine local cache
cache.getOrLoad("user:" + id, User.class, Duration.ofMinutes(10), () -> load(id));

// After adding quarkforge-redis, it automatically switches to a two-level cache: local → Redis → loader
```

### Security & Authentication (security)

```java
@Inject JwtTokenService jwt;
@Inject AuthContext auth;

// Issue a token at the login endpoint
String token = jwt.issue(user.id.toString(), user.name, Set.of("admin"));

// Read the current user in business endpoints; 401 is thrown automatically when not logged in
LoginUser current = auth.currentUser();
```

Signing/verification keys use the standard SmallRye JWT configuration (RSA key files recommended; never commit private keys in production):

```properties
smallrye.jwt.sign.key.location=jwt/privateKey.pem
mp.jwt.verify.publickey.location=jwt/publicKey.pem
mp.jwt.verify.issuer=quarkforge
```

### Rate Limiting (ratelimit)

```java
@POST
@RateLimit(permits = 5, windowSeconds = 60, key = KeyStrategy.USER)
public Result<Void> sendSmsCode() { ... }
```

Supports three dimensions: `USER` / `IP` / `GLOBAL`; excess requests get HTTP 429. The default in-memory implementation suits single-node deployments; adding `quarkforge-redis` automatically switches to Redis-based distributed rate limiting.

### Idempotency (idempotency)

```java
@POST
@Idempotent(key = "order:#req.orderNo", ttlSeconds = 600)
public Result<OrderVO> createOrder(@RequestBody CreateOrderRequest req) {
    // Duplicate requests with the same orderNo:
    // - The first request executes the business method and caches the return value for 600 seconds
    // - Within the TTL, the cached result is replayed without re-executing
    // - Concurrent requests with the same key get HTTP 409 (CONFLICT)
}
```

The `key` supports `#paramName` and `#p0` index references with chained `.field` access; when left empty, the key is generated from the serialized arguments. The default in-memory store suits single-node deployments; adding `quarkforge-redis` automatically switches to the distributed implementation. If the business method throws, the idempotency key is released immediately so later retries are allowed.

### Messaging (messaging + RabbitMQ)

No middleware required by default: messages are only written to memory and logged (`LocalMessagePublisher`), so local development works out of the box; adding `quarkforge-messaging-rabbitmq` automatically switches to real delivery.

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

`destination` is used as the routing key; if `exchange` is left empty, the message goes to the default exchange (in which case `destination` is the queue name). Consumers can use Quarkus-native `@Incoming` or a RabbitMQ client directly.

### Transactional Outbox (outbox)

Keep business writes and message sending in the same transaction, avoiding "order persisted but message never sent":

```java
@Transactional
public void createOrder(CreateOrderRequest request) {
    orderRepository.persist(order);
    outbox.enqueue("order.created", new OrderCreatedEvent(order.id, order.no));
}
```

Delayed delivery and failure retry:

```java
// Trigger an order-closing check 30 minutes later
outbox.enqueue("order.close-check", Map.of("orderNo", orderNo), Duration.ofMinutes(30));
```

A background job scans the local message table every 5 seconds for redelivery; failures back off exponentially as `30s → 60s → 120s …` and are marked `FAILED` after 5 attempts. In multi-instance deployments, messages are claimed via conditional updates, so each message is delivered by exactly one instance. For plain scheduled tasks, use Quarkus `@Scheduled` directly.

### RPC (rpc)

gRPC-based communication foundation for inter-service calls, built on `quarkus-grpc`. Service definitions live in `src/main/proto`; the Quarkus build generates blocking, async and Mutiny stubs.

**Server side**: implement the generated `*ImplBase` (or the Mutiny service interface) as a CDI bean annotated `@GrpcService` + `@Singleton`; it is exposed on the gRPC port (default 9000):

```java
@GrpcService
@Singleton
public class OrderGrpcService extends OrderGrpc.OrderImplBase {

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        // Throw BizException directly — the global interceptor maps it to a gRPC status
        Order order = orderRepository.findByIdOptional(request.getId())
                .orElseThrow(() -> new BizException(CommonErrorCode.NOT_FOUND));
        responseObserver.onNext(toResponse(order));
        responseObserver.onCompleted();
    }
}
```

**Client side**: inject stubs with `@GrpcClient` and configure the channel:

```java
@GrpcClient("order")
OrderGrpc.OrderBlockingStub orderStub;
```

```properties
quarkus.grpc.clients.order.host=order-service
quarkus.grpc.clients.order.port=9000
```

**Unified error mapping**: a server-side `BizException` is converted to the matching gRPC status by its HTTP-aligned code segment (40400 → NOT_FOUND, 40100 → UNAUTHENTICATED, 42900 → RESOURCE_EXHAUSTED, …), with the original numeric code carried in the `x-error-code` trailer. Clients recover it with [RpcErrors](quarkforge-rpc/src/main/java/io/github/foundry4j/quarkforge/rpc/RpcErrors.java):

```java
try {
    orderStub.get(request);
} catch (StatusRuntimeException e) {
    throw RpcErrors.toBizException(e);   // back to BizException with the original error code
}
```

Unhandled server exceptions map to `INTERNAL` without leaking internal details in the status description. Note the mapping applies to exceptions thrown directly by the service method (ImplBase style, or Mutiny methods that throw synchronously) rather than failed `Uni`s.

**Trace propagation**: with tracing enabled (default), the client interceptor forwards the current MDC `traceId` as the `x-trace-id` metadata header, and the server interceptor restores it into MDC — RPC logs line up with HTTP logs under the same trace id.

**Diagnostics**: the built-in `Echo` service returns the message it receives plus the trace id observed on the server, handy for verifying connectivity and interceptor wiring after deployment.

### Redis (redis)

```java
// Cache-aside
User user = cache.getOrLoad("user:" + id, User.class, Duration.ofMinutes(10),
        () -> userRepository.requireById(id));

// Distributed lock
lock.withLock("order:pay:" + orderId, Duration.ofSeconds(30), () -> doPay());
```

### Observability (observability)

Works out of the box: Swagger UI at `/q/swagger-ui` (with a built-in JWT bearerAuth debugging scheme), health checks at `/q/health`, Prometheus metrics at `/q/metrics`.

## Example Service

`examples/user-service` is a sample user microservice built on this framework, wiring together the web / data / security / ratelimit / observability modules:

| Capability | Where it lands in the example |
|------------|-------------------------------|
| User CRUD | [UserResource](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserResource.java) + [UserService](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserService.java) + [UserRepository](examples/user-service/src/main/java/io/github/foundry4j/quarkforge/sample/user/UserRepository.java) |
| Unified responses | Resource methods return DTOs directly, automatically wrapped in the `Result` envelope |
| Exceptions & validation | Duplicate username throws `BizException`; request-body validation failures automatically return 400 |
| Login & auth | `JwtTokenService` issues JWTs; `/users/me` is protected by `@RolesAllowed` |
| Rate limiting | Login endpoint: `@RateLimit(permits = 20, windowSeconds = 60)` |
| Docs & monitoring | Swagger UI, `/q/health`, `/q/metrics` |

Endpoint overview:

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users` | Register a user |
| GET | `/users?keyword=&page=&size=` | Keyword search with pagination |
| GET | `/users/{id}` | User details; 404 when not found |
| PUT | `/users/{id}` | Update profile or status |
| DELETE | `/users/{id}` | Delete a user (204) |
| POST | `/auth/login` | Log in to obtain a JWT |
| GET | `/users/me` | Current logged-in user; requires `Authorization: Bearer <token>` |

Run locally:

```bash
mvn -pl examples/user-service -am install -DskipTests
mvn -pl examples/user-service quarkus:dev
```

The example uses in-memory H2 and demo JWT keys from the repository, so it runs out of the box; replace the data source and keys in production.

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `quarkforge.web.response.auto-wrap` | `true` | Automatic response-body wrapping switch |
| `quarkforge.web.access-log.enabled` | `true` | Access log switch |
| `quarkforge.web.file.base-dir` | `${java.io.tmpdir}/quarkforge-files` | File storage root directory |
| `quarkforge.web.file.max-size` | `10M` | Per-file size limit; excess returns 413 |
| `quarkforge.web.file.allowed-content-types` | empty (unrestricted) | Allowed upload MIME types, comma-separated |
| `quarkforge.web.version.enabled` | `true` | API version handling switch |
| `quarkforge.web.version.default` | `v1` | Default version when none is specified |
| `quarkforge.rpc.trace.enabled` | `true` | gRPC traceId propagation switch (client + server interceptors) |
| `quarkforge.data.worker-id` | `0` | Snowflake ID node number ([0, 1023]) |
| `quarkforge.data.tenant.default` | `default` | Tenant ID used when none is set explicitly |
| `quarkforge.audit.enabled` | `true` | Audit log persistence switch |
| `quarkforge.crypto.key` | none (required) | Field encryption key, Base64 of 32 bytes |
| `quarkforge.cache.local.max-size` | `10000` | Maximum number of local cache entries |
| `quarkforge.cache.local.default-ttl` | `PT5M` | Default local cache TTL |
| `quarkforge.jwt.issuer` | `quarkforge` | JWT issuer |
| `quarkforge.jwt.ttl-minutes` | `120` | JWT validity (minutes) |
| `quarkforge.outbox.enabled` | `true` | Outbox background dispatch switch |
| `quarkforge.outbox.dispatch-interval` | `5s` | Outbox scan interval |
| `quarkforge.outbox.max-attempts` | `5` | Maximum delivery attempts per message |
| `quarkforge.outbox.backoff-seconds` | `30` | Initial failure backoff in seconds (doubles exponentially afterwards) |

package io.github.foundry4j.quarkforge.degrade;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// ## DegradeIntegrationTest
/// 验证 SmallRye Fault Tolerance 注解与统一 Result 异常体系的打通效果：
/// 重试/降级/熔断/超时的行为，以及 FT 基础设施异常的 HTTP 响应形态。
@QuarkusTest
class DegradeIntegrationTest {

    @Inject
    FlakyService service;

    // ---------- 被测服务与资源 ----------

    @ApplicationScoped
    public static class FlakyService {

        // static：CDI 客户端代理只代理方法调用，实例字段会读到代理自身的副本
        static final AtomicInteger retryAttempts = new AtomicInteger();
        static final AtomicInteger bizAttempts = new AtomicInteger();

        @Retry(maxRetries = 2)
        public String retryThenOk() {
            if (retryAttempts.incrementAndGet() < 3) {
                throw new IllegalStateException("transient");
            }
            return "ok";
        }

        /// 业务异常是预期内失败：abortOn 后不重试、不浪费配额（框架推荐用法）
        @Retry(maxRetries = 2, abortOn = BizException.class)
        public String retryOnBusinessError() {
            bizAttempts.incrementAndGet();
            throw new BizException(CommonErrorCode.NOT_FOUND);
        }

        @Fallback(fallbackMethod = "fallback")
        public String alwaysFail() {
            throw new IllegalStateException("down");
        }

        String fallback() {
            return "fallback-value";
        }

        @CircuitBreaker(requestVolumeThreshold = 2, failureRatio = 0.5, delay = 60000)
        public String breaker() {
            throw new IllegalStateException("downstream down");
        }

        @Timeout(100)
        public String slow() throws InterruptedException {
            Thread.sleep(2000);
            return "never";
        }
    }

    @Path("/degrade-test")
    @ApplicationScoped
    public static class DegradeResource {

        @Inject
        FlakyService service;

        @GET
        @Path("/breaker")
        public String breaker() {
            return service.breaker();
        }

        @GET
        @Path("/slow")
        public String slow() throws InterruptedException {
            return service.slow();
        }
    }

    @BeforeEach
    void reset() {
        FlakyService.retryAttempts.set(0);
        FlakyService.bizAttempts.set(0);
    }

    // ---------- 注解行为 ----------

    @Test
    void retryRecoversAfterTransientFailures() {
        assertEquals("ok", service.retryThenOk());
        assertEquals(3, FlakyService.retryAttempts.get());
    }

    @Test
    void businessExceptionAbortsRetry() {
        BizException ex = assertThrows(BizException.class, () -> service.retryOnBusinessError());
        assertEquals(CommonErrorCode.NOT_FOUND.code(), ex.code());
        assertEquals(1, FlakyService.bizAttempts.get(), "BizException 不应被重试");
    }

    @Test
    void fallbackProvidesDegradedValue() {
        assertEquals("fallback-value", service.alwaysFail());
    }

    // ---------- Result 异常体系打通 ----------

    @Test
    void circuitOpenMapsTo503WithUnifiedBody() {
        // 前两次真实失败（原始异常经兜底映射为 500）
        given().get("/degrade-test/breaker").then().statusCode(500);
        given().get("/degrade-test/breaker").then().statusCode(500);

        // 熔断打开：快速失败 → 503 + 统一 Result 体
        given().get("/degrade-test/breaker")
                .then()
                .statusCode(503)
                .contentType(ContentType.JSON)
                .body("code", equalTo(50300));
    }

    @Test
    void timeoutMapsTo504WithUnifiedBody() {
        given().get("/degrade-test/slow")
                .then()
                .statusCode(504)
                .contentType(ContentType.JSON)
                .body("code", equalTo(50400));
    }
}

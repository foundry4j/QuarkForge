package io.github.foundry4j.quarkforge.rpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoGrpc;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoRequest;
import io.github.foundry4j.quarkforge.rpc.grpc.EchoResponse;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Echo 服务的端到端测试：验证错误映射、错误码 trailer 与 traceId 链路透传。
@QuarkusTest
class EchoServiceTest {

    @GrpcClient("echo")
    EchoGrpc.EchoBlockingStub echo;

    @Test
    void echoesMessage() {
        EchoResponse response = echo.ping(EchoRequest.newBuilder().setMessage("hello").build());
        assertEquals("hello", response.getMessage());
        assertEquals("", response.getTraceId());
    }

    @Test
    void propagatesTraceIdThroughInterceptors() {
        MDC.put(RpcTracing.TRACE_ID_MDC_KEY, "trace-test-1");
        try {
            EchoResponse response = echo.ping(EchoRequest.newBuilder().setMessage("ping").build());
            assertEquals("trace-test-1", response.getTraceId());
        } finally {
            MDC.remove(RpcTracing.TRACE_ID_MDC_KEY);
        }
    }

    @Test
    void mapsBizExceptionToGrpcStatusWithErrorCode() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> echo.ping(EchoRequest.newBuilder().setMessage("throw:biz").build()));

        assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
        assertEquals(40400, RpcErrors.errorCode(exception).orElseThrow());

        BizException restored = RpcErrors.toBizException(exception);
        assertEquals(40400, restored.code());
        assertEquals("echo resource not found", restored.getMessage());
    }

    @Test
    void mapsUnhandledExceptionToInternal() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> echo.ping(EchoRequest.newBuilder().setMessage("throw:boom").build()));

        assertEquals(Status.Code.INTERNAL, exception.getStatus().getCode());
        assertEquals(50000, RpcErrors.toBizException(exception).code());
    }
}

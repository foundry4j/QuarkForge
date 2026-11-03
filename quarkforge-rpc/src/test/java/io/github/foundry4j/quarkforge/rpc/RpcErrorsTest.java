package io.github.foundry4j.quarkforge.rpc;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// RpcErrors 双向映射的纯单元测试。
class RpcErrorsTest {

    @Test
    void mapsErrorCodeSegmentsToGrpcStatus() {
        assertEquals(Status.Code.INVALID_ARGUMENT, RpcErrors.toStatusCode(40000));
        assertEquals(Status.Code.INVALID_ARGUMENT, RpcErrors.toStatusCode(41500));
        assertEquals(Status.Code.UNAUTHENTICATED, RpcErrors.toStatusCode(40100));
        assertEquals(Status.Code.PERMISSION_DENIED, RpcErrors.toStatusCode(40300));
        assertEquals(Status.Code.NOT_FOUND, RpcErrors.toStatusCode(40400));
        assertEquals(Status.Code.ABORTED, RpcErrors.toStatusCode(40900));
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, RpcErrors.toStatusCode(41300));
        assertEquals(Status.Code.RESOURCE_EXHAUSTED, RpcErrors.toStatusCode(42900));
        assertEquals(Status.Code.INTERNAL, RpcErrors.toStatusCode(50000));
        assertEquals(Status.Code.UNAVAILABLE, RpcErrors.toStatusCode(50300));
        assertEquals(Status.Code.UNKNOWN, RpcErrors.toStatusCode(999));
    }

    @Test
    void mapsBizExceptionToStatusWithMessage() {
        Status status = RpcErrors.toStatus(new BizException(CommonErrorCode.NOT_FOUND));
        assertEquals(Status.Code.NOT_FOUND, status.getCode());
        assertEquals(CommonErrorCode.NOT_FOUND.message(), status.getDescription());
    }

    @Test
    void mapsUnhandledExceptionToInternalWithoutLeakingDetails() {
        Status status = RpcErrors.toStatus(new IllegalStateException("secret internals"));
        assertEquals(Status.Code.INTERNAL, status.getCode());
        assertEquals(CommonErrorCode.INTERNAL_ERROR.message(), status.getDescription());
    }

    @Test
    void passesStatusRuntimeExceptionThrough() {
        StatusRuntimeException original = Status.PERMISSION_DENIED.withDescription("denied").asRuntimeException();
        assertEquals(Status.Code.PERMISSION_DENIED, RpcErrors.toStatus(original).getCode());
        assertEquals("denied", RpcErrors.toStatus(original).getDescription());
    }

    @Test
    void extractsErrorCodeFromTrailers() {
        Metadata trailers = new Metadata();
        trailers.put(RpcErrors.ERROR_CODE_KEY, "40400");
        assertEquals(40400, RpcErrors.errorCode(trailers).orElseThrow());
    }

    @Test
    void returnsEmptyWhenErrorCodeMissingOrInvalid() {
        assertTrue(RpcErrors.errorCode((Metadata) null).isEmpty());
        assertTrue(RpcErrors.errorCode(new Metadata()).isEmpty());
        Metadata invalid = new Metadata();
        invalid.put(RpcErrors.ERROR_CODE_KEY, "not-a-number");
        assertTrue(RpcErrors.errorCode(invalid).isEmpty());
    }

    @Test
    void restoresBizExceptionFromTrailersErrorCode() {
        Metadata trailers = new Metadata();
        trailers.put(RpcErrors.ERROR_CODE_KEY, "42900");
        StatusRuntimeException exception = Status.RESOURCE_EXHAUSTED
                .withDescription("slow down")
                .asRuntimeException(trailers);

        BizException restored = RpcErrors.toBizException(exception);
        assertEquals(42900, restored.code());
        assertEquals("slow down", restored.getMessage());
    }

    @Test
    void fallsBackToStatusCodeMappingWhenNoErrorCodeTrailer() {
        StatusRuntimeException exception = Status.NOT_FOUND.asRuntimeException();
        BizException restored = RpcErrors.toBizException(exception);
        assertEquals(CommonErrorCode.NOT_FOUND.code(), restored.code());

        StatusRuntimeException unavailable = Status.UNAVAILABLE.asRuntimeException();
        assertEquals(CommonErrorCode.SERVICE_UNAVAILABLE.code(),
                RpcErrors.toBizException(unavailable).code());
    }
}

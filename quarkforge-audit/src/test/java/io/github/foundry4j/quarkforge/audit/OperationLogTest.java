package io.github.foundry4j.quarkforge.audit;

import io.github.foundry4j.quarkforge.core.api.PageResult;
import io.github.foundry4j.quarkforge.core.context.ActorContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OperationLogTest {

    @Inject
    OperationLogFixture fixture;

    @Inject
    OperationLogRepository repository;

    @AfterEach
    void cleanup() {
        ActorContext.clear();
        fixture.clear();
    }

    @Test
    void recordsSuccessfulInvocationWithActorAndArgs() {
        String result = ActorContext.callAs("alice", () -> fixture.create("SO-1", 100));

        assertEquals("created:SO-1", result);
        List<OperationLogEntry> entries = repository.listAll();
        assertEquals(1, entries.size());
        OperationLogEntry entry = entries.getFirst();
        assertEquals("order", entry.module);
        assertEquals("create", entry.action);
        assertEquals("创建订单", entry.description);
        assertEquals("alice", entry.actor);
        assertEquals("OperationLogFixture#create", entry.method);
        assertEquals("[\"SO-1\",100]", entry.args);
        assertTrue(entry.success);
        assertNull(entry.errorMessage);
        assertTrue(entry.durationMs >= 0);
        assertNotNull(entry.occurredAt);
        assertNull(entry.requestMethod);
        assertNull(entry.ip);
    }

    @Test
    void recordsFailureEvenWhenBusinessTransactionRollsBack() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ActorContext.runAs("bob", () -> fixture.payAndFail("SO-2")));

        assertEquals("payment failed", ex.getMessage());
        List<OperationLogEntry> entries = repository.listAll();
        assertEquals(1, entries.size());
        OperationLogEntry entry = entries.getFirst();
        assertEquals("bob", entry.actor);
        assertFalse(entry.success);
        assertTrue(entry.errorMessage.contains("payment failed"));
        assertTrue(entry.args.contains("SO-2"));
    }

    @Test
    void recordArgsCanBeDisabled() {
        ActorContext.runAs("carol", () -> fixture.remove("SO-3"));

        OperationLogEntry entry = repository.listAll().getFirst();
        assertEquals("carol", entry.actor);
        assertNull(entry.args);
        assertTrue(entry.success);
    }

    @Test
    void fallsBackToClassAndMethodName() {
        fixture.plain();

        OperationLogEntry entry = repository.listAll().getFirst();
        assertEquals("OperationLogFixture", entry.module);
        assertEquals("plain", entry.action);
        assertEquals("system", entry.actor);
    }

    @Test
    void queriesByActorModuleAndFailures() {
        ActorContext.runAs("alice", () -> fixture.create("SO-4", 1));
        ActorContext.runAs("bob", () -> fixture.remove("SO-5"));
        assertThrows(IllegalStateException.class,
                () -> ActorContext.runAs("bob", () -> fixture.payAndFail("SO-6")));

        PageResult<OperationLogEntry> byActor = repository.pageByActor("bob", 1, 10);
        assertEquals(2, byActor.total());
        assertEquals(2, byActor.records().size());

        PageResult<OperationLogEntry> byModule = repository.pageByModule("order", 1, 10);
        assertEquals(3, byModule.total());

        PageResult<OperationLogEntry> failures = repository.pageFailures(1, 10);
        assertEquals(1, failures.total());
        assertFalse(failures.records().getFirst().success);
    }
}

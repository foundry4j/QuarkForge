package io.github.foundry4j.quarkforge.audit;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.github.foundry4j.quarkforge.core.context.ActorContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuditTest {

    @Inject
    AuditFixture fixture;

    @AfterEach
    void cleanup() {
        ActorContext.clear();
        fixture.clear();
    }

    @Test
    void recordsCreateUpdateAndDeleteWithActorAndFieldDiff() {
        Long id = ActorContext.callAs("alice", () -> fixture.create("widget", 100));

        List<AuditLog> afterCreate = fixture.logs(id);
        assertEquals(1, afterCreate.size());
        assertEquals("CREATE", afterCreate.getFirst().action);
        assertEquals("alice", afterCreate.getFirst().actor);
        assertTrue(afterCreate.getFirst().changes.contains("\"price\""));

        ActorContext.runAs("bob", () -> fixture.updatePrice(id, 120));

        List<AuditLog> afterUpdate = fixture.logs(id);
        assertEquals(2, afterUpdate.size());
        AuditLog update = afterUpdate.getLast();
        assertEquals("UPDATE", update.action);
        assertEquals("bob", update.actor);
        assertTrue(update.changes.contains("\"field\":\"price\""));
        assertTrue(update.changes.contains("\"oldValue\":\"100\""));
        assertTrue(update.changes.contains("\"newValue\":\"120\""));

        ActorContext.runAs("carol", () -> fixture.remove(id));

        List<AuditLog> afterDelete = fixture.logs(id);
        assertEquals(3, afterDelete.size());
        AuditLog delete = afterDelete.getLast();
        assertEquals("DELETE", delete.action);
        assertEquals("carol", delete.actor);
        assertTrue(delete.changes.contains("\"state\""));
    }

    @Test
    void updateWithoutChangesProducesNoAuditRecord() {
        Long id = ActorContext.callAs("alice", () -> fixture.create("widget", 100));

        ActorContext.runAs("bob", () -> fixture.updatePrice(id, 100));

        assertEquals(1, fixture.logs(id).size());
    }
}

package io.github.tuyucheng777.quarkforge.data;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.tuyucheng777.quarkforge.core.context.ActorContext;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TenantAndSoftDeleteTest {

    @Inject
    DataFixture fixture;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        ActorContext.clear();
        fixture.clearAll();
    }

    @Test
    void tenantIdIsFilledAndQueriesAreIsolated() {
        Long alphaId = TenantContext.callAs("alpha", () -> fixture.createTenantNote("alpha-note"));
        Long betaId = TenantContext.callAs("beta", () -> fixture.createTenantNote("beta-note"));

        assertEquals("alpha", TenantContext.callAs("alpha", () -> fixture.tenantIdOf(alphaId)));
        assertEquals("beta", TenantContext.callAs("beta", () -> fixture.tenantIdOf(betaId)));

        assertEquals(List.of("alpha-note"), TenantContext.callAs("alpha", fixture::tenantNoteTitles));
        assertEquals(List.of("beta-note"), TenantContext.callAs("beta", fixture::tenantNoteTitles));

        assertNotNull(TenantContext.callAs("alpha", () -> fixture.findTenantNote(alphaId)));
        assertNull(TenantContext.callAs("beta", () -> fixture.findTenantNote(alphaId)));
    }

    @Test
    void defaultTenantIsUsedWithoutContext() {
        Long id = fixture.createTenantNote("default-note");
        assertEquals(TenantContext.defaultTenantId(), fixture.tenantIdOf(id));
        assertEquals(List.of("default-note"), fixture.tenantNoteTitles());
    }

    @Test
    void softDeleteHidesRowsAndRestoreBringsThemBack() {
        Long id = fixture.createSoftNote("to-delete");
        fixture.createSoftNote("keep");

        ActorContext.runAs("alice", () -> assertTrue(fixture.deleteSoftNote(id)));

        assertEquals(List.of("keep"), fixture.activeSoftNoteTitles());
        List<String> all = fixture.allSoftNoteTitles();
        assertTrue(all.contains("to-delete"));

        SoftNote deleted = fixture.findSoftNoteIncludingDeleted(id);
        assertTrue(deleted.isDeleted());
        assertNotNull(deleted.deletedAt);
        assertEquals("alice", deleted.deletedBy);

        assertTrue(fixture.restoreSoftNote(id));
        assertEquals(List.of("keep", "to-delete"), fixture.activeSoftNoteTitles().stream().sorted().toList());
        assertEquals(List.of("keep"), fixture.activeSoftNoteTitles().stream()
                .filter(title -> title.equals("keep")).toList());
        assertFalse(fixture.findSoftNoteIncludingDeleted(id).isDeleted());
    }
}

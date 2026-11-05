package io.github.foundry4j.quarkforge.excel;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ExcelHttpTest {

    @Test
    void streamsCsvDownload() throws Exception {
        List<String> rows = List.of("alice", "bob");
        ExcelExportSpec<String> spec = ExcelExportSpec.<String>builder("Users")
                .column("name", name -> name)
                .build();

        Response response = ExcelHttp.download("users.csv", spec, rows);

        assertEquals(200, response.getStatus());
        assertEquals("text/csv;charset=UTF-8", response.getMediaType().toString());
        assertTrue(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION).contains("users.csv"));

        StreamingOutput body = (StreamingOutput) response.getEntity();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        body.write(out);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("name\n"));
        assertTrue(csv.contains("alice"));
    }

    @Test
    void streamsXlsxDownload() throws Exception {
        ExcelExportSpec<String> spec = ExcelExportSpec.<String>builder("Users")
                .column("name", name -> name)
                .build();

        Response response = ExcelHttp.download("users.xlsx", spec, List.of("alice"));

        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getMediaType().toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingOutput) response.getEntity()).write(out);
        assertTrue(out.size() > 1000);
    }
}

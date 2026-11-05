package io.github.foundry4j.quarkforge.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelReaderTest {

    @Test
    void readsXlsxWithHeadersAndSkipsBlankRows() {
        byte[] bytes = ExcelTestSupport.xlsx(new Object[][]{
                {"id", "name", "age"},
                {1, "alice", 30},
                null,
                {2, "bob", null}
        });

        List<RowData> rows = new ArrayList<>();
        ExcelReader.read(new ByteArrayInputStream(bytes), ExcelFormat.XLSX,
                RowReadOptions.defaults(), rows::add);

        assertEquals(2, rows.size());
        assertEquals(2, rows.getFirst().rowNumber());
        assertEquals("alice", rows.get(0).cell("name"));
        assertEquals("30", rows.get(0).cell("age"));
        assertEquals(4, rows.get(1).rowNumber());
        assertEquals("bob", rows.get(1).cell(1));
        assertEquals("", rows.get(1).cell("age"));
    }

    @Test
    void readsXlsxWithoutHeader() {
        byte[] bytes = ExcelTestSupport.xlsx(new Object[][]{
                {"id", "name"},
                {1, "alice"}
        });

        List<RowData> rows = new ArrayList<>();
        ExcelReader.read(new ByteArrayInputStream(bytes), ExcelFormat.XLSX,
                new RowReadOptions(0, -1, true, StandardCharsets.UTF_8), rows::add);

        assertEquals(2, rows.size());
        assertTrue(rows.getFirst().headers().isEmpty());
        assertEquals(1, rows.get(0).rowNumber());
        assertEquals("id", rows.get(0).cell(0));
        assertEquals("alice", rows.get(1).cell(1));
    }

    @Test
    void parsesCsvQuotesMultilineAndBom() {
        String csv = """
                \uFEFFid,name,note\r
                1,"Smith, John","line1
                line2"\r
                \r
                2,"He said ""hi""\",\r
                """;

        List<RowData> rows = new ArrayList<>();
        ExcelReader.read(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                ExcelFormat.CSV, RowReadOptions.defaults(), rows::add);

        assertEquals(2, rows.size());
        assertEquals(2, rows.getFirst().rowNumber());
        assertEquals("Smith, John", rows.get(0).cell("name"));
        assertEquals("line1\nline2", rows.get(0).cell("note"));
        assertEquals(4, rows.get(1).rowNumber());
        assertEquals("He said \"hi\"", rows.get(1).cell("name"));
        assertEquals("", rows.get(1).cell("note"));
    }

    @Test
    void detectsFormatFromFilename() {
        assertEquals(ExcelFormat.XLSX, ExcelFormat.fromFilename("users.xlsx"));
        assertEquals(ExcelFormat.XLSX, ExcelFormat.fromFilename("users.XLSM"));
        assertEquals(ExcelFormat.CSV, ExcelFormat.fromFilename("users.csv"));
        assertEquals(ExcelFormat.CSV, ExcelFormat.detect("blob", "text/csv;charset=UTF-8"));
        assertEquals(ExcelFormat.XLSX, ExcelFormat.detect("blob", ExcelFormat.XLSX.mediaType()));
    }
}

package io.github.foundry4j.quarkforge.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelExporterTest {

    record ExportRow(int id, String name, LocalDate date) {
    }

    private static final List<ExportRow> ROWS = IntStream.rangeClosed(1, 5000)
            .mapToObj(i -> new ExportRow(i, "user-" + i, LocalDate.of(2024, 2, 3)))
            .toList();

    private static ExcelExportSpec<ExportRow> spec() {
        return ExcelExportSpec.<ExportRow>builder("Users")
                .column("id", ExportRow::id, 12)
                .column("name", ExportRow::name, 24)
                .column("date", ExportRow::date, 16)
                .windowSize(100)
                .build();
    }

    @Test
    void streamsLargeXlsxWithoutRetainingAllRows() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.XLSX, spec(), ROWS);

        AtomicLong count = new AtomicLong();
        ExcelReader.read(new ByteArrayInputStream(out.toByteArray()), ExcelFormat.XLSX,
                RowReadOptions.defaults(), row -> count.incrementAndGet());

        assertEquals(ROWS.size(), count.get());
    }

    @Test
    void exportedXlsxKeepsCellTypesAndValues() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.XLSX, spec(), ROWS.subList(0, 2));

        String[][] cells = new String[2][];
        ExcelReader.read(new ByteArrayInputStream(out.toByteArray()), ExcelFormat.XLSX,
                RowReadOptions.defaults(), row -> cells[row.rowNumber() - 2] =
                        new String[]{row.cell("id"), row.cell("name"), row.cell("date")});

        assertEquals("1", cells[0][0]);
        assertEquals("user-1", cells[0][1]);
        assertEquals("2024-02-03", cells[0][2]);
        assertEquals("2", cells[1][0]);
    }

    @Test
    void exportsCsvWithBomAndEscaping() {
        record CsvRow(int id, String text) {
        }
        List<CsvRow> rows = List.of(
                new CsvRow(1, "a,b"),
                new CsvRow(2, "line1\nline2"),
                new CsvRow(3, "He said \"hi\""));

        ExcelExportSpec<CsvRow> csvSpec = ExcelExportSpec.<CsvRow>builder("Sheet1")
                .column("id", CsvRow::id)
                .column("text", CsvRow::text)
                .build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.CSV, csvSpec, rows);
        String csv = out.toString(StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"a,b\""));
        assertTrue(csv.contains("\"line1\nline2\""));
        assertTrue(csv.contains("\"He said \"\"hi\"\"\""));

        List<RowData> parsed = new ArrayList<>();
        ExcelReader.read(new ByteArrayInputStream(out.toByteArray()), ExcelFormat.CSV,
                RowReadOptions.defaults(), parsed::add);
        assertEquals(3, parsed.size());
        assertEquals("a,b", parsed.get(0).cell("text"));
        assertEquals("line1\nline2", parsed.get(1).cell("text"));
    }

    @Test
    void streamOverloadExportsAllRows() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelExporter.write(out, ExcelFormat.CSV, spec(), ROWS.stream().limit(3));

        long lines = out.toString(StandardCharsets.UTF_8).lines().count();
        assertEquals(4, lines);
    }
}

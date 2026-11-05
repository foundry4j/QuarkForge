package io.github.foundry4j.quarkforge.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelImporterTest {

    record User(String id, String name, int age) {
    }

    private static ExcelImportSpec<User> spec(List<User> sink, int maxErrors) {
        return ExcelImportSpec.<User>builder()
                .maxErrors(maxErrors)
                .mapper(row -> new User(row.cell("id"), row.cell("name"),
                        Integer.parseInt(row.cell("age"))))
                .validator((user, ctx) -> {
                    if (user.name().isBlank()) {
                        ctx.reject("name", user.name(), "名称不能为空");
                    }
                    if (user.age() < 1 || user.age() > 120) {
                        ctx.reject("age", String.valueOf(user.age()), "年龄超出范围");
                    }
                })
                .rowSink(sink::add)
                .build();
    }

    @Test
    void collectsSuccessfulRowsAndReportsFailedRows() {
        byte[] bytes = ExcelTestSupport.xlsx(new Object[][]{
                {"id", "name", "age"},
                {"1", "alice", "30"},
                {"2", "", "30"},
                {"3", "carol", "200"},
                {"4", "dave", "abc"}
        });

        List<User> users = new ArrayList<>();
        ImportResult result = ExcelImporter.importData(
                new ByteArrayInputStream(bytes), ExcelFormat.XLSX, spec(users, 500));

        assertEquals(4, result.totalRows());
        assertEquals(1, result.successRows());
        assertEquals(3, result.failedRows());
        assertTrue(result.hasErrors());
        assertFalse(result.errorsTruncated());
        assertEquals(3, result.errors().size());
        assertEquals(List.of(new User("1", "alice", 30)), users);

        RowError nameError = result.errors().getFirst();
        assertEquals(3, nameError.rowNumber());
        assertEquals("name", nameError.field());
        assertEquals(3, nameError.rowValues().size());

        RowError ageError = result.errors().get(1);
        assertEquals("age", ageError.field());
        assertEquals("200", ageError.value());

        RowError mappingError = result.errors().get(2);
        assertEquals(5, mappingError.rowNumber());
        assertTrue(mappingError.message().startsWith("映射失败"));
    }

    @Test
    void truncatesErrorDetailsButKeepsAccurateCounts() {
        byte[] bytes = ExcelTestSupport.xlsx(new Object[][]{
                {"id", "name", "age"},
                {"1", "", "30"},
                {"2", "", "30"},
                {"3", "", "30"}
        });

        ImportResult result = ExcelImporter.importData(
                new ByteArrayInputStream(bytes), ExcelFormat.XLSX, spec(new ArrayList<>(), 1));

        assertEquals(3, result.failedRows());
        assertEquals(1, result.errors().size());
        assertTrue(result.errorsTruncated());
    }

    @Test
    void importsCsvByFilename() {
        String csv = "id,name,age\n4,eve,28\n";
        List<User> users = new ArrayList<>();

        ImportResult result = ExcelImporter.importData(
                new ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "users.csv", spec(users, 0));

        assertEquals(1, result.successRows());
        assertEquals(List.of(new User("4", "eve", 28)), users);
    }

    @Test
    void treatsNullMappingAsRowError() {
        ExcelImportSpec<User> nullMapper = ExcelImportSpec.<User>builder()
                .mapper(row -> null)
                .build();

        ImportResult result = ExcelImporter.importData(
                new ByteArrayInputStream(ExcelTestSupport.xlsx(new Object[][]{
                        {"id", "name", "age"},
                        {"1", "alice", "30"}
                })), ExcelFormat.XLSX, nullMapper);

        assertEquals(1, result.totalRows());
        assertEquals(0, result.successRows());
        assertEquals(1, result.failedRows());
        assertEquals("映射结果为空", result.errors().getFirst().message());
    }
}

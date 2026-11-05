package io.github.foundry4j.quarkforge.excel;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/// ## ExcelExporter
/// 流式导出，内存占用由滑动窗口控制，与总行数无关：
///
/// - `.xlsx` 使用 POI `SXSSFWorkbook`，每写满 `windowSize` 行就刷到临时文件
/// - `.csv` 直接逐行写 `OutputStream`，并可选写 UTF-8 BOM
///
/// ```java
/// ExcelExporter.write(response, ExcelFormat.XLSX,
///         ExcelExportSpec.<Order>builder("Orders")
///                 .column("订单号", Order::orderNo)
///                 .column("金额", Order::amount)
///                 .build(),
///         orderStream);
/// ```
public final class ExcelExporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ExcelExporter() {
    }

    /// 按文件名后缀推断格式后导出。
    public static <T> void write(OutputStream out, String filename, ExcelExportSpec<T> spec,
                                 Iterable<? extends T> rows) {
        write(out, ExcelFormat.fromFilename(filename), spec, rows);
    }

    /// 流式导出 `Iterable` 数据。
    public static <T> void write(OutputStream out, ExcelFormat format, ExcelExportSpec<T> spec,
                                 Iterable<? extends T> rows) {
        if (out == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "缺少输出流");
        }
        if (format == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "缺少导出格式");
        }
        if (spec == null || rows == null) {
            throw new IllegalArgumentException("spec 与 rows 不能为空");
        }
        switch (format) {
            case XLSX -> writeXlsx(out, spec, rows);
            case CSV -> writeCsv(out, spec, rows);
        }
    }

    /// 流式导出 `Stream` 数据（不会主动关闭 stream）。
    public static <T> void write(OutputStream out, ExcelFormat format, ExcelExportSpec<T> spec,
                                 Stream<T> rows) {
        write(out, format, spec, rows::iterator);
    }

    // ---------- XLSX ----------

    private static <T> void writeXlsx(OutputStream out, ExcelExportSpec<T> spec,
                                      Iterable<? extends T> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(spec.windowSize())) {
            try {
                workbook.setCompressTempFiles(true);
                Sheet sheet = workbook.createSheet(spec.sheetName());

                CellStyle headerStyle = headerStyle(workbook);
                CellStyle dateStyle = dateStyle(workbook, "yyyy-mm-dd");
                CellStyle dateTimeStyle = dateStyle(workbook, "yyyy-mm-dd HH:mm:ss");

                Row header = sheet.createRow(0);
                for (int i = 0; i < spec.columns().size(); i++) {
                    Column<T> column = spec.columns().get(i);
                    Cell cell = header.createCell(i);
                    cell.setCellValue(column.header());
                    cell.setCellStyle(headerStyle);
                    sheet.setColumnWidth(i, Math.clamp(column.width(), 8, 60) * 256);
                }
                if (spec.freezeHeader()) {
                    sheet.createFreezePane(0, 1);
                }

                int rowIndex = 1;
                for (T item : rows) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int i = 0; i < spec.columns().size(); i++) {
                        Object value = spec.columns().get(i).getter().apply(item);
                        if (value != null) {
                            setCellValue(row.createCell(i), value, dateStyle, dateTimeStyle);
                        }
                    }
                }

                workbook.write(out);
                out.flush();
            } catch (IOException e) {
                throw new BizException(CommonErrorCode.INTERNAL_ERROR, "导出 Excel 失败", e);
            }
        } catch (IOException ignored) {
            // 关闭失败不影响已写出的内容
        }
        // close() also disposes the temporary files created by SXSSF
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static CellStyle dateStyle(Workbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(pattern));
        return style;
    }

    private static void setCellValue(Cell cell, Object value, CellStyle dateStyle, CellStyle dateTimeStyle) {
        if (value instanceof String text) {
            cell.setCellValue(text);
        } else if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
        } else if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            cell.setCellValue(((Number) value).longValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime);
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof Instant instant) {
            cell.setCellValue(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof java.sql.Date date) {
            cell.setCellValue(date.toLocalDate());
            cell.setCellStyle(dateStyle);
        } else if (value instanceof java.util.Date date) {
            cell.setCellValue(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            cell.setCellStyle(dateTimeStyle);
        } else if (value instanceof Enum<?> enumValue) {
            cell.setCellValue(enumValue.name());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    // ---------- CSV ----------

    private static <T> void writeCsv(OutputStream out, ExcelExportSpec<T> spec,
                                     Iterable<? extends T> rows) {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        try {
            if (spec.csvBom()) {
                writer.write('\uFEFF');
            }
            writeCsvLine(writer, spec.columns().stream()
                    .map(Column::header).map(Object.class::cast).toList());
            for (T item : rows) {
                Object[] values = new Object[spec.columns().size()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = spec.columns().get(i).getter().apply(item);
                }
                writeCsvLine(writer, java.util.Arrays.asList(values));
            }
            writer.flush();
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.INTERNAL_ERROR, "导出 CSV 失败", e);
        }
    }

    private static void writeCsvLine(Writer writer, java.util.List<?> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escapeCsv(csvValue(values.get(i))));
        }
        writer.write('\n');
    }

    private static String csvValue(Object value) {
        return switch (value) {
            case null -> "";
            case LocalDate date -> DATE_FORMAT.format(date);
            case LocalDateTime dateTime -> DATE_TIME_FORMAT.format(dateTime);
            case Instant instant -> DATE_TIME_FORMAT.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
            case java.sql.Timestamp timestamp -> DATE_TIME_FORMAT.format(timestamp.toLocalDateTime());
            case java.sql.Date date -> DATE_FORMAT.format(date.toLocalDate());
            case java.util.Date date -> DATE_TIME_FORMAT.format(date.toInstant().atZone(ZoneId.systemDefault()));
            case Enum<?> enumValue -> enumValue.name();
            default -> String.valueOf(value);
        };
    }

    private static String escapeCsv(String value) {
        boolean quote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0
                || value.startsWith(" ")
                || value.endsWith(" ");
        return quote ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}

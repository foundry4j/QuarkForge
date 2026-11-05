package io.github.foundry4j.quarkforge.excel;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// ## ExcelReader
/// 流式读取 `.xlsx` / `.csv`，逐行回调，内存占用与文件大小无关：
///
/// - Excel 走 POI 事件模型（SAX），不构建 Workbook
/// - CSV 使用字符级状态机，支持引号包裹的逗号/换行、`""` 转义、UTF-8 BOM
///
/// ```java
/// ExcelReader.read(in, ExcelFormat.XLSX, RowReadOptions.defaults(), row -> {
///     System.out.println(row.cell("name"));
/// });
/// ```
public final class ExcelReader {

    private ExcelReader() {
    }

    /// 按文件名后缀识别格式后读取。
    public static void read(InputStream in, String filename, RowReadOptions options, RowConsumer consumer) {
        read(in, ExcelFormat.fromFilename(filename), options, consumer);
    }

    /// 流式读取，`consumer` 每收到一行调用一次。
    public static void read(InputStream in, ExcelFormat format, RowReadOptions options, RowConsumer consumer) {
        if (in == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "缺少待读取的文件流");
        }
        if (format == null) {
            throw new BizException(CommonErrorCode.PARAM_ERROR, "缺少文件格式");
        }
        RowReadOptions opts = options == null ? RowReadOptions.defaults() : options;
        RowConsumer target = consumer == null ? _ -> {
        } : consumer;
        try {
            switch (format) {
                case XLSX -> readXlsx(in, opts, target);
                case CSV -> readCsv(in, opts, target);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(CommonErrorCode.PARAM_ERROR,
                    "读取 " + format + " 文件失败", e);
        }
    }

    // ---------- Excel（SAX） ----------

    private static void readXlsx(InputStream in, RowReadOptions options, RowConsumer consumer) throws Exception {
        try (OPCPackage pkg = OPCPackage.open(in)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStrings sharedStrings = reader.getSharedStringsTable();
            XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(
                    reader.getStylesTable(), sharedStrings,
                    new SheetHandler(options, consumer), false);

            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(handler);

            try (InputStream sheet = openSheet(reader, options.sheetIndex())) {
                parser.parse(new InputSource(sheet));
            }
        }
    }

    private static InputStream openSheet(XSSFReader reader, int sheetIndex) throws Exception {
        Iterator<InputStream> sheets = reader.getSheetsData();
        for (int i = 0; i < sheetIndex && sheets.hasNext(); i++) {
            try (InputStream ignored = sheets.next()) {
                // 跳过前置工作表，关闭其数据流避免句柄泄漏
            }
        }
        if (!sheets.hasNext()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR,
                    "工作表不存在，下标: " + sheetIndex);
        }
        return sheets.next();
    }

    /// POI SAX 回调：把单元格拼成一行文本，并按需处理表头/空行。
    private static final class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final RowReadOptions options;
        private final RowConsumer consumer;
        private final List<String> headers = new ArrayList<>();
        private List<String> current = new ArrayList<>();
        private int lastColumn;

        private SheetHandler(RowReadOptions options, RowConsumer consumer) {
            this.options = options;
            this.consumer = consumer;
        }

        @Override
        public void startRow(int rowNum) {
            current = new ArrayList<>();
            lastColumn = -1;
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int column = columnIndex(cellReference, current.size());
            while (current.size() <= column) {
                current.add("");
            }
            current.set(column, formattedValue == null ? "" : formattedValue);
            lastColumn = column;
        }

        @Override
        public void endRow(int rowNum) {
            int expectedWidth = Math.max(lastColumn + 1, current.size());
            while (current.size() < expectedWidth) {
                current.add("");
            }
            if (options.headerRowIndex() >= 0 && rowNum == options.headerRowIndex()) {
                headers.clear();
                headers.addAll(current);
                return;
            }
            if (options.headerRowIndex() >= 0 && rowNum < options.headerRowIndex()) {
                return;
            }
            if (options.skipEmptyRows() && isEmpty(current)) {
                return;
            }
            consumer.accept(new RowData(rowNum + 1, headers, current));
        }

        private static boolean isEmpty(List<String> values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // 忽略页眉页脚
        }
    }

    /// `A1` → 0，`AB12` → 27；解析失败时回退到当前列宽。
    private static int columnIndex(String cellReference, int fallback) {
        if (cellReference == null || cellReference.isEmpty()) {
            return fallback;
        }
        int column = 0;
        int seenLetter = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char c = cellReference.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                column = column * 26 + (c - 'A' + 1);
                seenLetter++;
            } else if (c >= 'a' && c <= 'z') {
                column = column * 26 + (c - 'a' + 1);
                seenLetter++;
            } else {
                break;
            }
        }
        return seenLetter == 0 ? fallback : Math.max(0, column - 1);
    }

    // ---------- CSV ----------

    private static void readCsv(InputStream in, RowReadOptions options, RowConsumer consumer) throws IOException {
        try (PushbackReader reader = new PushbackReader(
                new InputStreamReader(in, options.charset()), 2)) {
            int first = reader.read();
            if (first != 0xFEFF && first != -1) {
                reader.unread(first);
            }
            new CsvParser(options, consumer).parse(reader);
        }
    }

    /// CSV 字符级状态机：流式解析，支持多行引号字段与 BOM。
    private static final class CsvParser {

        private final RowReadOptions options;
        private final RowConsumer consumer;
        private final List<String> headers = new ArrayList<>();

        private List<String> fields = new ArrayList<>();
        private final StringBuilder field = new StringBuilder();
        private boolean inQuotes;
        private boolean recordStarted;
        private int recordIndex;

        private CsvParser(RowReadOptions options, RowConsumer consumer) {
            this.options = options;
            this.consumer = consumer;
        }

        private void parse(PushbackReader reader) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                char ch = (char) c;
                if (inQuotes) {
                    if (ch == '"') {
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                            recordStarted = true;
                        } else {
                            inQuotes = false;
                            if (next != -1) {
                                reader.unread(next);
                            }
                        }
                    } else {
                        field.append(ch);
                    }
                } else if (ch == '"') {
                    inQuotes = true;
                    recordStarted = true;
                } else if (ch == ',') {
                    fields.add(field.toString());
                    field.setLength(0);
                    recordStarted = true;
                } else if (ch == '\n') {
                    emitRecord();
                } else if (ch == '\r') {
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.unread(next);
                    }
                    emitRecord();
                } else {
                    field.append(ch);
                    recordStarted = true;
                }
            }
            if (inQuotes) {
                throw new BizException(CommonErrorCode.PARAM_ERROR, "CSV 格式错误：存在未闭合的引号");
            }
            if (recordStarted || !fields.isEmpty() || !field.isEmpty()) {
                emitRecord();
            }
        }

        private void emitRecord() {
            fields.add(field.toString());
            field.setLength(0);
            int logicalRow = recordIndex + 1;

            if (options.headerRowIndex() >= 0 && recordIndex == options.headerRowIndex()) {
                headers.clear();
                headers.addAll(fields);
            } else if (options.headerRowIndex() < 0 || recordIndex > options.headerRowIndex()) {
                if (!(options.skipEmptyRows() && isEmpty(fields))) {
                    consumer.accept(new RowData(logicalRow, headers, fields));
                }
            }

            fields = new ArrayList<>();
            recordIndex++;
            recordStarted = false;
        }

        private static boolean isEmpty(List<String> values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
            return true;
        }
    }
}

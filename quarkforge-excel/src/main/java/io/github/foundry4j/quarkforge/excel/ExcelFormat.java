package io.github.foundry4j.quarkforge.excel;

import io.github.foundry4j.quarkforge.core.api.CommonErrorCode;
import io.github.foundry4j.quarkforge.core.exception.BizException;

import java.util.Locale;

/// ## ExcelFormat
/// 支持的文件格式。Excel 仅支持 OOXML（`.xlsx` / `.xlsm`），旧版 `.xls` 请先另存为 `.xlsx`。
public enum ExcelFormat {

    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("text/csv");

    private final String mediaType;

    ExcelFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    public String mediaType() {
        return mediaType;
    }

    /// 按文件名后缀识别格式；无法识别时抛出 400 业务异常。
    public static ExcelFormat fromFilename(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xlsm")) {
            return XLSX;
        }
        if (lower.endsWith(".csv")) {
            return CSV;
        }
        throw new BizException(CommonErrorCode.PARAM_ERROR,
                "不支持的文件类型，仅支持 .xlsx/.xlsm/.csv");
    }

    /// 优先按 Content-Type 识别，其次回退到文件名后缀。
    public static ExcelFormat detect(String filename, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("csv")) {
            return CSV;
        }
        if (type.contains("spreadsheetml") || type.contains("ms-excel")) {
            return XLSX;
        }
        return fromFilename(filename);
    }
}

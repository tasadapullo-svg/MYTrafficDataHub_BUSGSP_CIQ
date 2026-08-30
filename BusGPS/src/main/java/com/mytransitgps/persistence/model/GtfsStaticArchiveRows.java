package com.mytransitgps.persistence.model;

import java.util.List;
import java.util.Map;

/**
 * 中文名称：GTFS Static 原始 CSV 行集合。
 *
 * 功能说明：完整保留 ZIP 中各核心 txt 文件的原始字符串字段，
 * 用于 source_row JSONB 和冻结数据库字段映射；既有 GtfsStaticParser 仍负责业务解析语义。
 */
public record GtfsStaticArchiveRows(Map<String, List<Map<String, String>>> rowsByFile) {

    public List<Map<String, String>> rows(String fileName) {
        return rowsByFile.getOrDefault(fileName, List.of());
    }

    public boolean present(String fileName) {
        return rowsByFile.containsKey(fileName);
    }
}

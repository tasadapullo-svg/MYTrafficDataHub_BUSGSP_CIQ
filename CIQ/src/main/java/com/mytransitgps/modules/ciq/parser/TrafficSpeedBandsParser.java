package com.mytransitgps.modules.ciq.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * API01 TrafficSpeedBands JSON 解析器。
 *
 * <p>解析器只适配 LTA 真实字段并执行宽松类型转换，允许数字以 String 或 JSON Number 出现；
 * 质量判断、重复判断和空间筛选交给后续 QC 与 PostGIS 环节处理。</p>
 */
public class TrafficSpeedBandsParser {
    private final ObjectMapper objectMapper;

    public TrafficSpeedBandsParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析完整 LTA 响应页，支持 OData 的 value 数组和直接数组两种形态。
     */
    public List<TrafficSpeedBandRecord> parse(byte[] body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        JsonNode rows = root != null && root.isArray() ? root : root == null ? null : root.get("value");
        if (rows == null || !rows.isArray()) {
            return List.of();
        }
        List<TrafficSpeedBandRecord> records = new ArrayList<>(rows.size());
        for (JsonNode row : rows) {
            records.add(new TrafficSpeedBandRecord(
                    text(row, "LinkID"),
                    text(row, "RoadName"),
                    shortValue(row, "RoadCategory"),
                    shortValue(row, "SpeedBand"),
                    shortValue(row, "MinimumSpeed"),
                    shortValue(row, "MaximumSpeed"),
                    decimal(row, "StartLon"),
                    decimal(row, "StartLat"),
                    decimal(row, "EndLon"),
                    decimal(row, "EndLat")
            ));
        }
        return records;
    }

    private static String text(JsonNode row, String field) {
        JsonNode node = row == null ? null : row.get(field);
        if (node == null || node.isNull()) return null;
        String value = node.asText();
        return value == null ? null : value.trim();
    }

    private static Short shortValue(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null || value.isBlank()) return null;
        try {
            return Short.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal decimal(JsonNode row, String field) {
        String value = text(row, field);
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}


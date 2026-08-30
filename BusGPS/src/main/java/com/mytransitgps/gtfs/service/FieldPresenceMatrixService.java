package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mytransitgps.gtfs.model.FieldPresenceAudit;
import com.mytransitgps.gtfs.util.GtfsTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 字段存在性矩阵输出服务，生成便于人工检查的 CSV 与 JSON 报告。
 */
public class FieldPresenceMatrixService {

    private static final Logger log = LoggerFactory.getLogger(FieldPresenceMatrixService.class);
    private final JsonOutputService jsonOutputService;

    public FieldPresenceMatrixService(JsonOutputService jsonOutputService) {
        this.jsonOutputService = jsonOutputService;
    }

    public MatrixPaths writeMatrix(
            Path jsonRoot,
            String timestamp,
            List<FieldPresenceAudit> audits,
            Map<String, String> displayNameByFeedId) throws IOException {
        Set<String> fields = new LinkedHashSet<>();
        for (FieldPresenceAudit audit : audits) {
            fields.addAll(audit.fields().keySet());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> csvLines = new ArrayList<>();
        List<String> header = new ArrayList<>();
        header.add("field");
        for (String feedId : displayNameByFeedId.keySet()) {
            String display = displayNameByFeedId.get(feedId);
            header.add(display + " count");
            header.add(display + " total");
            header.add(display + " percent");
        }
        csvLines.add(String.join(",", header));

        for (String field : fields) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("field", field);
            List<String> csvRow = new ArrayList<>();
            csvRow.add(field);
            for (String feedId : displayNameByFeedId.keySet()) {
                FieldPresenceAudit audit = audits.stream()
                        .filter(item -> item.feedId().equals(feedId))
                        .findFirst()
                        .orElse(new FieldPresenceAudit.Builder(feedId, 0).build());
                FieldPresenceAudit.FieldPresenceCount count = audit.fields().get(field);
                int present = count == null ? 0 : count.presentCount();
                int total = count == null ? audit.vehicleCount() : count.vehicleCount();
                double percent = count == null ? 0.0d : count.percentage();
                row.put(feedId + "_count", present);
                row.put(feedId + "_total", total);
                row.put(feedId + "_percent", percent);
                csvRow.add(Integer.toString(present));
                csvRow.add(Integer.toString(total));
                csvRow.add(String.format(java.util.Locale.ROOT, "%.2f", percent));
            }
            rows.add(row);
            csvLines.add(String.join(",", csvRow));
        }

        Path jsonPath = jsonRoot.resolve("multi_city_field_presence_matrix_" + timestamp + ".json");
        Path csvPath = jsonRoot.resolve("multi_city_field_presence_matrix_" + timestamp + ".csv");
        jsonOutputService.writeJsonObject(jsonPath, Map.of("generated_at", GtfsTime.formatFileTimestamp(java.time.Instant.now()), "rows", rows));
        java.nio.file.Files.createDirectories(csvPath.getParent());
        java.nio.file.Files.writeString(csvPath, String.join(System.lineSeparator(), csvLines) + System.lineSeparator());
        log.info("字段存在性矩阵生成完成，feedCount={}，fieldCount={}，jsonPath={}，csvPath={}",
                displayNameByFeedId.size(), fields.size(), jsonPath, csvPath);
        return new MatrixPaths(jsonPath, csvPath);
    }

    /** 字段存在性矩阵生成后的 JSON 与 CSV 文件路径。 */
    public record MatrixPaths(Path jsonPath, Path csvPath) {
    }
}

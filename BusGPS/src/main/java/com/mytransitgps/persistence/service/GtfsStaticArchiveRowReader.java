package com.mytransitgps.persistence.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.mytransitgps.persistence.model.GtfsStaticArchiveRows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：GTFS Static 原始行读取器。
 *
 * 功能说明：在复用既有 GtfsStaticParser 业务结果的同时，读取同一 ZIP 的原始 CSV 行，
 * 为 PostgreSQL source_row 和未进入既有精简模型的标准字段提供忠实输入。
 */
public class GtfsStaticArchiveRowReader {

    private static final Logger log = LoggerFactory.getLogger(GtfsStaticArchiveRowReader.class);
    private static final Set<String> FILES = Set.of(
            "routes.txt", "trips.txt", "stops.txt", "stop_times.txt", "shapes.txt",
            "calendar.txt", "calendar_dates.txt", "frequencies.txt");
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(false)
            .setTrim(false)
            .get();

    public GtfsStaticArchiveRows read(Path zipPath) {
        log.info("开始读取Static GTFS原始CSV行，zipPath={}", zipPath);
        Map<String, List<Map<String, String>>> rows = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String fileName = Path.of(entry.getName()).getFileName().toString();
                if (!entry.isDirectory() && FILES.contains(fileName)) {
                    rows.put(fileName, parse(zip.readAllBytes()));
                }
            }
            int totalRows = rows.values().stream().mapToInt(List::size).sum();
            log.info("Static GTFS原始CSV行读取完成，zipPath={}，files={}，rows={}", zipPath, rows.size(), totalRows);
            return new GtfsStaticArchiveRows(Map.copyOf(rows));
        } catch (IOException ex) {
            log.error("读取Static GTFS原始CSV行失败，zipPath={}，错误信息={}", zipPath, ex.getMessage());
            throw new IllegalStateException("Failed to read Static CSV rows from " + zipPath, ex);
        }
    }

    private List<Map<String, String>> parse(byte[] bytes) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
             CSVParser parser = FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                record.toMap().forEach((key, value) -> row.put(key, value == null || value.isEmpty() ? null : value));
                rows.add(Collections.unmodifiableMap(row));
            }
        }
        return List.copyOf(rows);
    }
}

package com.mytransitgps.modules.ciqbus.service;

import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusPassage;
import com.mytransitgps.modules.ciqbus.persistence.CiqBusRealtimeRepository;
import com.mytransitgps.modules.ciqbus.persistence.CiqBusRealtimeRepository.CrossingRow;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiqBusDailyExportService {
    private static final Logger log = LoggerFactory.getLogger(CiqBusDailyExportService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CiqBusRealtimeRepository repository;
    private final CiqBusProperties properties;
    private final ZoneId zoneId;

    public CiqBusDailyExportService(CiqBusRealtimeRepository repository, CiqBusProperties properties) {
        this.repository = repository;
        this.properties = properties;
        this.zoneId = ZoneId.of(properties.getTimezone());
    }

    public Path exportPreviousDay(Instant now) {
        return export(LocalDate.ofInstant(now, zoneId).minusDays(1));
    }

    public Path export(LocalDate date) {
        log.info("[CIQBUS-XLSX] 项目进度：开始生成CIQBus每日Excel，serviceDate={}", date);
        repository.upsertDailySummary(date);
        List<CiqBusPassage> passages = repository.passagesForDate(date, zoneId);
        List<CrossingRow> crossings = repository.crossingsForDate(date);
        Path output = Path.of(properties.getStorage().getRoot()).resolve("CIQBus_" + date + ".xlsx");
        log.info("[CIQBUS-XLSX] 项目进度：Excel数据准备完成，serviceDate={}，过站明细数={}，完整过境事件数={}，output={}",
                date, passages.size(), crossings.size(), output);
        try {
            Files.createDirectories(output.getParent());
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                writePassages(workbook, date, passages);
                writeCrossings(workbook, date, crossings);
                writeHourly(workbook, crossings);
                writeDaily(workbook, crossings);
                try (OutputStream out = Files.newOutputStream(output)) {
                    workbook.write(out);
                }
            }
            log.info("[CIQBUS-XLSX] 项目进度：CIQBus每日Excel写入完成，output={}", output);
            return output;
        } catch (IOException ex) {
            throw new IllegalStateException("CIQBus XLSX export failed: " + output, ex);
        }
    }

    private void writePassages(XSSFWorkbook workbook, LocalDate date, List<CiqBusPassage> rows) {
        Sheet sheet = workbook.createSheet("车辆站点明细");
        header(sheet, "日期", "matched_event_id", "route_no", "operator_code", "direction_code",
                "stop_sequence", "stop_code", "stop_name", "passage_type", "pass_time",
                "estimated_arrival", "latitude", "longitude", "observation_count", "confidence", "match_method");
        int r = 1;
        for (CiqBusPassage p : rows) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(date.toString());
            row.createCell(c++).setCellValue(p.matchedEventId());
            row.createCell(c++).setCellValue(p.routeNo());
            row.createCell(c++).setCellValue(nvl(p.operatorCode()));
            row.createCell(c++).setCellValue(p.directionCode());
            row.createCell(c++).setCellValue(p.stopSequence() == null ? 0 : p.stopSequence());
            row.createCell(c++).setCellValue(p.stopCode());
            row.createCell(c++).setCellValue(nvl(p.stopName()));
            row.createCell(c++).setCellValue(p.passageType());
            row.createCell(c++).setCellValue(format(p.passTime()));
            row.createCell(c++).setCellValue(format(p.estimatedArrival()));
            row.createCell(c++).setCellValue(p.latitude() == null ? "" : p.latitude().toString());
            row.createCell(c++).setCellValue(p.longitude() == null ? "" : p.longitude().toString());
            row.createCell(c++).setCellValue(p.observationCount());
            row.createCell(c++).setCellValue(p.confidence());
            row.createCell(c).setCellValue(nvl(p.matchMethod()));
        }
        autosize(sheet, 16);
    }

    private void writeCrossings(XSSFWorkbook workbook, LocalDate date, List<CrossingRow> rows) {
        Sheet sheet = workbook.createSheet("完整过境事件");
        header(sheet, "日期", "matched_event_id", "route_no", "direction", "entry_stop_code",
                "ciq_entry_time", "exit_stop_code", "ciq_exit_time", "crossing_seconds",
                "crossing_minutes", "observation_count", "confidence", "event_status");
        int r = 1;
        for (CrossingRow x : rows) {
            Row row = sheet.createRow(r++);
            int c = 0;
            row.createCell(c++).setCellValue(date.toString());
            row.createCell(c++).setCellValue(x.matchedEventId());
            row.createCell(c++).setCellValue(x.routeNo());
            row.createCell(c++).setCellValue(x.directionCode());
            row.createCell(c++).setCellValue(x.entryStopCode());
            row.createCell(c++).setCellValue(format(x.entryTime()));
            row.createCell(c++).setCellValue(x.exitStopCode());
            row.createCell(c++).setCellValue(format(x.exitTime()));
            row.createCell(c++).setCellValue(x.crossingSeconds());
            row.createCell(c++).setCellValue(x.crossingSeconds() / 60.0d);
            row.createCell(c++).setCellValue(x.observationCount());
            row.createCell(c++).setCellValue(x.confidence());
            row.createCell(c).setCellValue(x.eventStatus());
        }
        autosize(sheet, 13);
    }

    private void writeHourly(XSSFWorkbook workbook, List<CrossingRow> rows) {
        Sheet sheet = workbook.createSheet("小时统计");
        header(sheet, "小时", "SG_TO_JB车辆次数", "JB_TO_SG车辆次数", "总车辆次数", "平均crossing_seconds", "median", "P90", "P95");
        Map<Integer, List<CrossingRow>> byHour = rows.stream().collect(Collectors.groupingBy(r -> r.entryTime().atZone(zoneId).getHour()));
        int rowIndex = 1;
        for (int hour = 0; hour < 24; hour++) {
            List<CrossingRow> hourRows = byHour.getOrDefault(hour, List.of());
            Row row = sheet.createRow(rowIndex++);
            List<Integer> seconds = seconds(hourRows);
            row.createCell(0).setCellValue(String.format("%02d:00", hour));
            row.createCell(1).setCellValue(hourRows.stream().filter(r -> "SG_TO_JB".equals(r.directionCode())).count());
            row.createCell(2).setCellValue(hourRows.stream().filter(r -> "JB_TO_SG".equals(r.directionCode())).count());
            row.createCell(3).setCellValue(hourRows.size());
            row.createCell(4).setCellValue(avg(seconds));
            row.createCell(5).setCellValue(percentile(seconds, 0.5));
            row.createCell(6).setCellValue(percentile(seconds, 0.9));
            row.createCell(7).setCellValue(percentile(seconds, 0.95));
        }
        autosize(sheet, 8);
    }

    private void writeDaily(XSSFWorkbook workbook, List<CrossingRow> rows) {
        Sheet sheet = workbook.createSheet("每日汇总");
        header(sheet, "160数量", "170数量", "170X数量", "950数量", "SG_TO_JB总数", "JB_TO_SG总数",
                "双向总数", "平均crossing_seconds", "median", "P90", "P95", "最短", "最长", "最拥堵小时");
        Row row = sheet.createRow(1);
        List<Integer> seconds = seconds(rows);
        row.createCell(0).setCellValue(routeCount(rows, "160"));
        row.createCell(1).setCellValue(routeCount(rows, "170"));
        row.createCell(2).setCellValue(routeCount(rows, "170X"));
        row.createCell(3).setCellValue(routeCount(rows, "950"));
        row.createCell(4).setCellValue(rows.stream().filter(r -> "SG_TO_JB".equals(r.directionCode())).count());
        row.createCell(5).setCellValue(rows.stream().filter(r -> "JB_TO_SG".equals(r.directionCode())).count());
        row.createCell(6).setCellValue(rows.size());
        row.createCell(7).setCellValue(avg(seconds));
        row.createCell(8).setCellValue(percentile(seconds, 0.5));
        row.createCell(9).setCellValue(percentile(seconds, 0.9));
        row.createCell(10).setCellValue(percentile(seconds, 0.95));
        row.createCell(11).setCellValue(seconds.stream().min(Integer::compareTo).orElse(0));
        row.createCell(12).setCellValue(seconds.stream().max(Integer::compareTo).orElse(0));
        row.createCell(13).setCellValue(busiestHour(rows));
        autosize(sheet, 14);
    }

    private void header(Sheet sheet, String... names) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < names.length; i++) {
            row.createCell(i).setCellValue(names[i]);
        }
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private List<Integer> seconds(List<CrossingRow> rows) {
        return rows.stream().map(CrossingRow::crossingSeconds).sorted().toList();
    }

    private double avg(List<Integer> values) {
        return values.isEmpty() ? 0 : values.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private double percentile(List<Integer> values, double p) {
        if (values.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(values.size() * p) - 1;
        return values.get(Math.max(0, Math.min(values.size() - 1, index)));
    }

    private long routeCount(List<CrossingRow> rows, String route) {
        return rows.stream().filter(r -> route.equals(r.routeNo())).count();
    }

    private String busiestHour(List<CrossingRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(r -> r.entryTime().atZone(zoneId).getHour(), Collectors.counting()))
                .entrySet().stream().max(Comparator.comparingLong(Map.Entry::getValue))
                .map(e -> String.format("%02d:00", e.getKey())).orElse("");
    }

    private String format(Instant instant) {
        return instant == null ? "" : TS.format(instant.atZone(zoneId));
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}

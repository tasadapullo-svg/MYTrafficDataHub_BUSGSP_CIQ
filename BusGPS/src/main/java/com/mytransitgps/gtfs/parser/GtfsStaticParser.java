package com.mytransitgps.gtfs.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StopInfo;
import com.mytransitgps.gtfs.model.StopTimeInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static GTFS ZIP 解析器，读取核心 CSV 并构建可关联的内存索引。
 */
public class GtfsStaticParser {

    private static final Logger log = LoggerFactory.getLogger(GtfsStaticParser.class);
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    public StaticFeedData parse(byte[] zipBytes) throws IOException {
        // 先一次性读取 ZIP 条目，再按 GTFS 文件名解析，避免重复扫描压缩流。
        log.info("开始解析Static GTFS ZIP，zipBytes={}", zipBytes == null ? 0 : zipBytes.length);
        Map<String, byte[]> entries = unzip(zipBytes);
        List<String> missingCoreFiles = List.of("routes.txt", "trips.txt", "stops.txt", "stop_times.txt").stream()
                .filter(name -> !entries.containsKey(name)).toList();
        if (!missingCoreFiles.isEmpty()) {
            log.warn("Static GTFS缺少核心文件，missingFiles={}", missingCoreFiles);
        }

        Map<String, RouteInfo> routes = entries.containsKey("routes.txt")
                ? parseRoutes(entries.get("routes.txt"))
                : Map.of();
        Map<String, TripInfo> trips = entries.containsKey("trips.txt")
                ? parseTrips(entries.get("trips.txt"))
                : Map.of();
        Map<String, StopInfo> stops = entries.containsKey("stops.txt")
                ? parseStops(entries.get("stops.txt"))
                : Map.of();
        Map<String, List<StopTimeInfo>> stopTimesByTrip = entries.containsKey("stop_times.txt")
                ? parseStopTimes(entries.get("stop_times.txt"))
                : Map.of();
        Map<String, List<ShapePoint>> shapesById = entries.containsKey("shapes.txt")
                ? parseShapes(entries.get("shapes.txt"))
                : Map.of();
        int stopsCount = stops.size();
        int stopTimesCount = stopTimesByTrip.values().stream().mapToInt(List::size).sum();

        StaticFeedData result = new StaticFeedData(
                routes,
                trips,
                stops,
                stopTimesByTrip,
                shapesById,
                stopsCount,
                stopTimesCount,
                entries.containsKey("routes.txt"),
                entries.containsKey("trips.txt"),
                entries.containsKey("stops.txt"),
                entries.containsKey("stop_times.txt"),
                entries.containsKey("shapes.txt"),
                entries.containsKey("calendar.txt"),
                entries.containsKey("calendar_dates.txt"),
                entries.containsKey("frequencies.txt"));
        log.info("Static GTFS解析完成，routes={}，trips={}，stops={}，stopTimes={}，shapes={}，zipEntries={}",
                routes.size(), trips.size(), stopsCount, stopTimesCount, shapesById.size(), entries.size());
        return result;
    }

    private Map<String, byte[]> unzip(byte[] zipBytes) throws IOException {
        // 保留 ZIP 条目名，后续严格按 routes.txt、trips.txt 等标准文件路由。
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), zipInputStream.readAllBytes());
                }
                zipInputStream.closeEntry();
            }
        }
        return entries;
    }

    private Map<String, RouteInfo> parseRoutes(byte[] csvBytes) throws IOException {
        Map<String, RouteInfo> routes = new LinkedHashMap<>();
        for (CSVRecord record : parseCsv(csvBytes)) {
            String routeId = get(record, "route_id");
            if (routeId == null || routeId.isBlank()) {
                continue;
            }
            routes.put(routeId, new RouteInfo(
                    routeId,
                    get(record, "route_short_name"),
                    get(record, "route_long_name"),
                    get(record, "route_type"),
                    get(record, "route_color"),
                    get(record, "route_text_color")));
        }
        return routes;
    }

    private Map<String, TripInfo> parseTrips(byte[] csvBytes) throws IOException {
        Map<String, TripInfo> trips = new LinkedHashMap<>();
        for (CSVRecord record : parseCsv(csvBytes)) {
            String tripId = get(record, "trip_id");
            if (tripId == null || tripId.isBlank()) {
                continue;
            }
            trips.put(tripId, new TripInfo(
                    get(record, "route_id"),
                    get(record, "service_id"),
                    tripId,
                    get(record, "trip_headsign"),
                    get(record, "trip_short_name"),
                    parseInteger(get(record, "direction_id")),
                    get(record, "shape_id")));
        }
        return trips;
    }

    private Map<String, StopInfo> parseStops(byte[] csvBytes) throws IOException {
        Map<String, StopInfo> stops = new LinkedHashMap<>();
        for (CSVRecord record : parseCsv(csvBytes)) {
            String stopId = get(record, "stop_id");
            if (stopId == null || stopId.isBlank()) {
                continue;
            }
            stops.put(stopId, new StopInfo(
                    stopId,
                    get(record, "stop_name"),
                    parseDouble(get(record, "stop_lat")),
                    parseDouble(get(record, "stop_lon"))));
        }
        return stops;
    }

    private Map<String, List<StopTimeInfo>> parseStopTimes(byte[] csvBytes) throws IOException {
        Map<String, List<StopTimeInfo>> stopTimesByTrip = new LinkedHashMap<>();
        for (CSVRecord record : parseCsv(csvBytes)) {
            String tripId = get(record, "trip_id");
            if (tripId == null || tripId.isBlank()) {
                continue;
            }
            stopTimesByTrip.computeIfAbsent(tripId, ignored -> new ArrayList<>()).add(new StopTimeInfo(
                    tripId,
                    parseInteger(get(record, "stop_sequence")),
                    get(record, "stop_id"),
                    get(record, "arrival_time"),
                    get(record, "departure_time")));
        }
        for (List<StopTimeInfo> stopTimes : stopTimesByTrip.values()) {
            stopTimes.sort(Comparator.comparing(StopTimeInfo::stopSequence, Comparator.nullsLast(Integer::compareTo)));
        }
        return stopTimesByTrip;
    }

    private Map<String, List<ShapePoint>> parseShapes(byte[] csvBytes) throws IOException {
        Map<String, List<ShapePoint>> shapesById = new LinkedHashMap<>();
        for (CSVRecord record : parseCsv(csvBytes)) {
            String shapeId = get(record, "shape_id");
            if (shapeId == null || shapeId.isBlank()) {
                continue;
            }
            shapesById.computeIfAbsent(shapeId, ignored -> new ArrayList<>()).add(new ShapePoint(
                    shapeId,
                    parseInteger(get(record, "shape_pt_sequence")),
                    parseDouble(get(record, "shape_pt_lat")),
                    parseDouble(get(record, "shape_pt_lon"))));
        }
        return shapesById;
    }

    private List<CSVRecord> parseCsv(byte[] csvBytes) throws IOException {
        String text = new String(csvBytes, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        try (Reader reader = new StringReader(text);
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            return parser.getRecords();
        }
    }

    private String get(CSVRecord record, String fieldName) {
        Map<String, String> values = record.toMap();
        if (values.containsKey(fieldName)) {
            return emptyToNull(values.get(fieldName));
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String normalized = entry.getKey() == null ? "" : entry.getKey().replace("\uFEFF", "");
            if (fieldName.equals(normalized)) {
                return emptyToNull(entry.getValue());
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}

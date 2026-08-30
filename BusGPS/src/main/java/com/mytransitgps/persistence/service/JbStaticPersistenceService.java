package com.mytransitgps.persistence.service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StaticShapeQcResult;
import com.mytransitgps.gtfs.parser.GtfsStaticParser;
import com.mytransitgps.gtfs.service.FeedBoundsService;
import com.mytransitgps.gtfs.service.StaticShapeQcService;
import com.mytransitgps.gtfs.util.GeoUtils;
import com.mytransitgps.gtfs.util.GtfsTimeParser;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.JbStaticRouteEntity;
import com.mytransitgps.persistence.entity.JbStaticShapeEntity;
import com.mytransitgps.persistence.entity.JbStaticShapePointEntity;
import com.mytransitgps.persistence.entity.JbStaticStopEntity;
import com.mytransitgps.persistence.entity.JbStaticStopTimeEntity;
import com.mytransitgps.persistence.entity.JbStaticTripEntity;
import com.mytransitgps.persistence.entity.JbStaticVersionEntity;
import com.mytransitgps.persistence.mapper.JbStaticRouteMapper;
import com.mytransitgps.persistence.mapper.JbStaticShapeMapper;
import com.mytransitgps.persistence.mapper.JbStaticShapePointMapper;
import com.mytransitgps.persistence.mapper.JbStaticStopMapper;
import com.mytransitgps.persistence.mapper.JbStaticStopTimeMapper;
import com.mytransitgps.persistence.mapper.JbStaticTripMapper;
import com.mytransitgps.persistence.mapper.JbStaticVersionMapper;
import com.mytransitgps.persistence.model.GtfsStaticArchiveRows;
import com.mytransitgps.persistence.model.LocalStaticArtifact;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import com.mytransitgps.persistence.typehandler.PostgisGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 中文名称：Johor Bahru GTFS Static 数据库持久化服务。
 *
 * 功能说明：复用既有 GtfsStaticParser、StaticShapeQcService 和 FeedBoundsService，
 * 将本地已校验 ZIP 按冻结顺序一次性写入 jb 的七张 Static 表；一个版本使用一个事务。
 * 输入：本地 Static ZIP。输出：jb.static_version 及其结构化子表。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class JbStaticPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(JbStaticPersistenceService.class);
    private final ObjectMapper objectMapper;
    private final MyTransitGpsDatabaseProperties properties;
    private final JbStaticVersionMapper versionMapper;
    private final JbStaticRouteMapper routeMapper;
    private final JbStaticStopMapper stopMapper;
    private final JbStaticShapeMapper shapeMapper;
    private final JbStaticShapePointMapper shapePointMapper;
    private final JbStaticTripMapper tripMapper;
    private final JbStaticStopTimeMapper stopTimeMapper;
    private final StaticUidResolver uidResolver;

    public JbStaticPersistenceService(ObjectMapper objectMapper, MyTransitGpsDatabaseProperties properties,
                                      JbStaticVersionMapper versionMapper, JbStaticRouteMapper routeMapper,
                                      JbStaticStopMapper stopMapper, JbStaticShapeMapper shapeMapper,
                                      JbStaticShapePointMapper shapePointMapper, JbStaticTripMapper tripMapper,
                                      JbStaticStopTimeMapper stopTimeMapper, StaticUidResolver uidResolver) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.versionMapper = versionMapper;
        this.routeMapper = routeMapper;
        this.stopMapper = stopMapper;
        this.shapeMapper = shapeMapper;
        this.shapePointMapper = shapePointMapper;
        this.tripMapper = tripMapper;
        this.stopTimeMapper = stopTimeMapper;
        this.uidResolver = uidResolver;
    }

    @Transactional
    public PreparedDatabaseStatic prepare(Path workspaceRoot, UUID feedUid, String feedId, LocalStaticArtifact artifact) {
        // 同一 Feed + SHA 已存在时复用版本；新版本的所有 Static 表在一个事务中完整写入。
        try {
            byte[] zipBytes = java.nio.file.Files.readAllBytes(artifact.zipPath());
            StaticFeedData feedData = new GtfsStaticParser().parse(zipBytes);
            FeedSpatialBounds bounds = new FeedBoundsService(10.0d).compute(feedId, artifact.sha256(), feedData);
            JbStaticVersionEntity existing = uidResolver.findVersion(feedUid, artifact.sha256());
            if (existing != null) {
                log.info("Static版本复用完成，staticVersionUid={}，sha256={}", existing.uid, artifact.sha256());
                return new PreparedDatabaseStatic(artifact, feedData, bounds,
                        uidResolver.load(existing.uid, artifact.sha256()), true, false);
            }

            log.info("数据库中不存在对应Static版本，开始一次性结构化导入，sha256={}", artifact.sha256());
            GtfsStaticArchiveRows raw = new GtfsStaticArchiveRowReader().read(artifact.zipPath());
            UUID versionUid = UUID.randomUUID();
            insertVersion(workspaceRoot, feedUid, artifact, raw, versionUid);
            Map<String, UUID> routeUids = insertRoutes(raw.rows("routes.txt"), versionUid);
            Map<String, UUID> stopUids = insertStops(raw.rows("stops.txt"), versionUid);
            Map<String, UUID> shapeUids = insertShapesAndPoints(raw, feedData, feedId, versionUid);
            Map<String, UUID> tripUids = insertTrips(raw.rows("trips.txt"), versionUid, routeUids, shapeUids);
            insertStopTimes(raw.rows("stop_times.txt"), versionUid, tripUids, stopUids);

            StaticReferenceIndex references = new StaticReferenceIndex(
                    versionUid, artifact.sha256(), Map.copyOf(routeUids), Map.copyOf(tripUids),
                    Map.copyOf(stopUids), Map.copyOf(shapeUids));
            log.info("Static导入完成，staticVersionUid={}，routes={}，trips={}，stops={}，stopTimes={}，shapes={}，shapePoints={}",
                    versionUid, routeUids.size(), tripUids.size(), stopUids.size(), raw.rows("stop_times.txt").size(),
                    shapeUids.size(), uniqueShapePointRows(raw.rows("shapes.txt")).size());
            return new PreparedDatabaseStatic(artifact, feedData, bounds, references, false, true);
        } catch (Exception ex) {
            log.error("Static导入失败，sha256={}，错误信息={}", artifact.sha256(), ex.getMessage(), ex);
            throw new IllegalStateException("Static persistence failed.", ex);
        }
    }

    private void insertVersion(Path workspaceRoot, UUID feedUid, LocalStaticArtifact artifact,
                               GtfsStaticArchiveRows raw, UUID versionUid) {
        JbStaticVersionEntity row = new JbStaticVersionEntity();
        row.uid = versionUid;
        row.feedUid = feedUid;
        row.versionCode = "mybas-johor_" + artifact.sha256().substring(0, 12);
        row.staticSha256 = artifact.sha256();
        row.staticObjectPath = slash(workspaceRoot.relativize(artifact.zipPath()));
        row.zipSizeBytes = artifact.zipSizeBytes();
        row.downloadedAt = artifact.downloadedAt();
        row.isCurrent = true;
        row.routesFilePresent = raw.present("routes.txt");
        row.tripsFilePresent = raw.present("trips.txt");
        row.stopsFilePresent = raw.present("stops.txt");
        row.stopTimesFilePresent = raw.present("stop_times.txt");
        row.shapesFilePresent = raw.present("shapes.txt");
        row.calendarFilePresent = raw.present("calendar.txt");
        row.calendarDatesFilePresent = raw.present("calendar_dates.txt");
        row.frequenciesFilePresent = raw.present("frequencies.txt");
        row.routesCount = raw.rows("routes.txt").size();
        row.tripsCount = raw.rows("trips.txt").size();
        row.stopsCount = raw.rows("stops.txt").size();
        row.stopTimesCount = raw.rows("stop_times.txt").size();
        row.shapesCount = (int) raw.rows("shapes.txt").stream().map(value -> value.get("shape_id")).distinct().count();
        int uniqueShapePoints = uniqueShapePointRows(raw.rows("shapes.txt")).size();
        row.shapePointsCount = uniqueShapePoints;
        row.notes = "Imported from verified local Static ZIP; existing parser semantics reused. Exact duplicate shape points normalized to satisfy frozen unique key; raw ZIP preserved.";
        versionMapper.insert(row);
    }

    private Map<String, UUID> insertRoutes(List<Map<String, String>> rows, UUID versionUid) {
        List<JbStaticRouteEntity> entities = new ArrayList<>();
        Map<String, UUID> uids = new LinkedHashMap<>();
        for (Map<String, String> source : rows) {
            JbStaticRouteEntity row = new JbStaticRouteEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.routeId = source.get("route_id");
            row.agencyId = source.get("agency_id"); row.routeShortName = source.get("route_short_name");
            row.routeLongName = source.get("route_long_name"); row.routeDesc = source.get("route_desc");
            row.routeType = integer(source.get("route_type")); row.routeUrl = source.get("route_url");
            row.routeColor = source.get("route_color"); row.routeTextColor = source.get("route_text_color");
            row.routeSortOrder = integer(source.get("route_sort_order")); row.continuousPickup = integer(source.get("continuous_pickup"));
            row.continuousDropOff = integer(source.get("continuous_drop_off")); row.networkId = source.get("network_id");
            row.sourceRow = sourceJson(source); entities.add(row); uids.put(row.routeId, row.uid);
        }
        if (!entities.isEmpty()) routeMapper.insert(entities, properties.persistence.getStaticBatchSize());
        return uids;
    }

    private Map<String, UUID> insertStops(List<Map<String, String>> rows, UUID versionUid) {
        List<JbStaticStopEntity> entities = new ArrayList<>();
        Map<String, UUID> uids = new LinkedHashMap<>();
        for (Map<String, String> source : rows) {
            JbStaticStopEntity row = new JbStaticStopEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.stopId = source.get("stop_id");
            row.stopCode = source.get("stop_code"); row.stopName = source.get("stop_name"); row.ttsStopName = source.get("tts_stop_name");
            row.stopDesc = source.get("stop_desc"); row.stopLat = decimal(source.get("stop_lat")); row.stopLon = decimal(source.get("stop_lon"));
            row.zoneId = source.get("zone_id"); row.stopUrl = source.get("stop_url"); row.locationType = integer(source.get("location_type"));
            row.parentStation = source.get("parent_station"); row.stopTimezone = source.get("stop_timezone");
            row.wheelchairBoarding = integer(source.get("wheelchair_boarding")); row.levelId = source.get("level_id"); row.platformCode = source.get("platform_code");
            applyStaticPosition(row); row.sourceRow = sourceJson(source); entities.add(row); uids.put(row.stopId, row.uid);
        }
        if (!entities.isEmpty()) stopMapper.insert(entities, properties.persistence.getStaticBatchSize());
        return uids;
    }

    private Map<String, UUID> insertShapesAndPoints(GtfsStaticArchiveRows raw, StaticFeedData feedData,
                                                     String feedId, UUID versionUid) {
        StaticShapeQcResult qcResult = new StaticShapeQcService(10.0d).analyze(feedId, feedData);
        Map<String, Map<String, Object>> details = new LinkedHashMap<>();
        qcResult.shapeDetails().forEach(row -> details.put(String.valueOf(row.get("shape_id")), row));
        List<Map<String, String>> uniquePointRows = uniqueShapePointRows(raw.rows("shapes.txt"));
        int removedDuplicateKeys = raw.rows("shapes.txt").size() - uniquePointRows.size();
        if (removedDuplicateKeys > 0) log.warn("Static shapes.txt包含重复(shape_id,sequence)键，按冻结唯一约束保留源文件首条，重复键数={}，原始ZIP不修改", removedDuplicateKeys);
        Map<String, List<Map<String, String>>> rawByShape = new LinkedHashMap<>();
        uniquePointRows.forEach(row -> rawByShape.computeIfAbsent(row.get("shape_id"), ignored -> new ArrayList<>()).add(row));
        Set<String> referenced = new LinkedHashSet<>();
        Map<String, Integer> tripCounts = new LinkedHashMap<>();
        raw.rows("trips.txt").forEach(row -> { String shapeId = row.get("shape_id"); if (shapeId != null) { referenced.add(shapeId); tripCounts.merge(shapeId, 1, Integer::sum); } });

        List<JbStaticShapeEntity> shapes = new ArrayList<>();
        Map<String, UUID> shapeUids = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : rawByShape.entrySet()) {
            String shapeId = entry.getKey();
            List<Map<String, String>> points = entry.getValue();
            points.sort(Comparator.comparingInt(value -> integer(value.get("shape_pt_sequence"))));
            JbStaticShapeEntity row = new JbStaticShapeEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.shapeId = shapeId;
            row.isReferenced = referenced.contains(shapeId); row.tripCountUsingShape = tripCounts.getOrDefault(shapeId, 0); row.pointCount = points.size();
            Map<String, Object> detail = details.get(shapeId);
            row.shapeLengthKm = detail == null ? null : decimalObject(detail.get("shape_length_km"));
            row.shapeLengthM = row.shapeLengthKm == null ? null : row.shapeLengthKm * 1000.0d;
            List<String> flags = detail == null ? new ArrayList<>() : new ArrayList<>(stringList(detail.get("qc_flags")));
            if (!row.isReferenced && !flags.contains("ORPHAN_SHAPE")) flags.add("ORPHAN_SHAPE");
            if (flags.size() == 1 && flags.contains("VALID_SHAPE")) flags.clear();
            row.analysisEligible = flags.isEmpty(); row.qcFlags = objectMapper.valueToTree(flags); row.qcSummary = objectMapper.valueToTree(detail == null ? Map.of() : detail);
            List<double[]> coordinates = new ArrayList<>();
            for (Map<String, String> point : points) { Double lat = decimal(point.get("shape_pt_lat")); Double lon = decimal(point.get("shape_pt_lon")); if (validPosition(lat, lon) && !zeroZero(lat, lon)) coordinates.add(new double[]{lon, lat}); }
            row.geom = PostgisGeometry.lineString(coordinates); shapes.add(row); shapeUids.put(shapeId, row.uid);
        }
        if (!shapes.isEmpty()) shapeMapper.insert(shapes, properties.persistence.getStaticBatchSize());

        List<JbStaticShapePointEntity> points = new ArrayList<>();
        for (Map<String, String> source : uniquePointRows) {
            JbStaticShapePointEntity row = new JbStaticShapePointEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.shapeId = source.get("shape_id"); row.shapeUid = shapeUids.get(row.shapeId);
            row.shapePtLat = decimal(source.get("shape_pt_lat")); row.shapePtLon = decimal(source.get("shape_pt_lon")); row.shapePtSequence = integer(source.get("shape_pt_sequence"));
            row.shapeDistTraveled = decimal(source.get("shape_dist_traveled")); row.positionWgs84Valid = validPosition(row.shapePtLat, row.shapePtLon);
            row.zeroZeroPosition = zeroZero(row.shapePtLat, row.shapePtLon); row.spatialEligible = row.positionWgs84Valid && !row.zeroZeroPosition;
            List<String> flags = new ArrayList<>(); if (!row.positionWgs84Valid) flags.add("INVALID_WGS84"); if (row.zeroZeroPosition) flags.add("ZERO_ZERO_POINT");
            row.qcFlags = objectMapper.valueToTree(flags); row.geom = row.spatialEligible ? PostgisGeometry.point(row.shapePtLon, row.shapePtLat) : null;
            row.sourceRow = sourceJson(source); points.add(row);
        }
        if (!points.isEmpty()) shapePointMapper.insert(points, properties.persistence.getStaticBatchSize());
        return shapeUids;
    }

    private List<Map<String, String>> uniqueShapePointRows(List<Map<String, String>> rows) {
        Map<String, Map<String, String>> unique = new LinkedHashMap<>();
        for (Map<String, String> row : rows) {
            String key = row.get("shape_id") + "\u0000" + row.get("shape_pt_sequence");
            unique.putIfAbsent(key, row);
        }
        return List.copyOf(unique.values());
    }

    private Map<String, UUID> insertTrips(List<Map<String, String>> rows, UUID versionUid,
                                           Map<String, UUID> routeUids, Map<String, UUID> shapeUids) {
        List<JbStaticTripEntity> entities = new ArrayList<>();
        Map<String, UUID> uids = new LinkedHashMap<>();
        for (Map<String, String> source : rows) {
            JbStaticTripEntity row = new JbStaticTripEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.routeId = source.get("route_id"); row.routeUid = routeUids.get(row.routeId);
            row.shapeId = source.get("shape_id"); row.shapeUid = shapeUids.get(row.shapeId); row.serviceId = source.get("service_id"); row.tripId = source.get("trip_id");
            row.tripHeadsign = source.get("trip_headsign"); row.tripShortName = source.get("trip_short_name"); row.directionId = shortInteger(source.get("direction_id"));
            row.blockId = source.get("block_id"); row.wheelchairAccessible = integer(source.get("wheelchair_accessible")); row.bikesAllowed = integer(source.get("bikes_allowed"));
            row.sourceRow = sourceJson(source); entities.add(row); uids.put(row.tripId, row.uid);
        }
        if (!entities.isEmpty()) tripMapper.insert(entities, properties.persistence.getStaticBatchSize());
        return uids;
    }

    private void insertStopTimes(List<Map<String, String>> rows, UUID versionUid,
                                 Map<String, UUID> tripUids, Map<String, UUID> stopUids) {
        List<JbStaticStopTimeEntity> entities = new ArrayList<>();
        for (Map<String, String> source : rows) {
            JbStaticStopTimeEntity row = new JbStaticStopTimeEntity();
            row.uid = UUID.randomUUID(); row.staticVersionUid = versionUid; row.tripId = source.get("trip_id"); row.tripUid = tripUids.get(row.tripId);
            row.stopId = source.get("stop_id"); row.stopUid = stopUids.get(row.stopId); row.stopSequence = integer(source.get("stop_sequence"));
            row.arrivalTimeRaw = source.get("arrival_time"); row.arrivalSeconds = gtfsSeconds(row.arrivalTimeRaw); row.departureTimeRaw = source.get("departure_time"); row.departureSeconds = gtfsSeconds(row.departureTimeRaw);
            row.stopHeadsign = source.get("stop_headsign"); row.pickupType = integer(source.get("pickup_type")); row.dropOffType = integer(source.get("drop_off_type"));
            row.continuousPickup = integer(source.get("continuous_pickup")); row.continuousDropOff = integer(source.get("continuous_drop_off")); row.shapeDistTraveled = decimal(source.get("shape_dist_traveled"));
            row.timepoint = integer(source.get("timepoint")); row.sourceRow = sourceJson(source); entities.add(row);
        }
        if (!entities.isEmpty()) stopTimeMapper.insert(entities, properties.persistence.getStaticBatchSize());
    }

    private void applyStaticPosition(JbStaticStopEntity row) {
        row.positionPresent = row.stopLat != null && row.stopLon != null;
        row.positionWgs84Valid = validPosition(row.stopLat, row.stopLon);
        row.zeroZeroPosition = zeroZero(row.stopLat, row.stopLon);
        row.spatialEligible = row.positionWgs84Valid && !row.zeroZeroPosition;
        List<String> flags = new ArrayList<>(); if (row.positionPresent && !row.positionWgs84Valid) flags.add("INVALID_WGS84"); if (row.zeroZeroPosition) flags.add("ZERO_ZERO_POSITION");
        row.qcFlags = objectMapper.valueToTree(flags); row.geom = row.spatialEligible ? PostgisGeometry.point(row.stopLon, row.stopLat) : null;
    }

    private JsonNode sourceJson(Map<String, String> source) { return objectMapper.valueToTree(source); }
    private Integer integer(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value.trim()); } catch (NumberFormatException ex) { return null; } }
    private Short shortInteger(String value) { Integer parsed = integer(value); return parsed == null ? null : parsed.shortValue(); }
    private Double decimal(String value) { try { return value == null || value.isBlank() ? null : Double.valueOf(value.trim()); } catch (NumberFormatException ex) { return null; } }
    private Double decimalObject(Object value) { return value instanceof Number number ? number.doubleValue() : decimal(value == null ? null : value.toString()); }
    @SuppressWarnings("unchecked") private List<String> stringList(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of(); }
    private Integer gtfsSeconds(String value) { try { return value == null ? null : new GtfsTimeParser().parseToSeconds(value); } catch (RuntimeException ex) { return null; } }
    private boolean validPosition(Double lat, Double lon) { return lat != null && lon != null && Double.isFinite(lat) && Double.isFinite(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180; }
    private boolean zeroZero(Double lat, Double lon) { return lat != null && lon != null && Double.compare(lat, 0.0d) == 0 && Double.compare(lon, 0.0d) == 0; }
    private String slash(Path path) { return path.toString().replace('\\', '/'); }
}

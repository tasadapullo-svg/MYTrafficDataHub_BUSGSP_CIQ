package com.mytransitgps.dashboard.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.dashboard.dto.DashboardDtos;
import com.mytransitgps.dashboard.model.DashboardCity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
/**
 * BUS GPS 大屏只读数据访问仓库。
 *
 * <p>通过 Dashboard 专用 JdbcTemplate 查询各城市 GPS 观测、最新状态、QC 和 API 请求证据；
 * CIQ SQL 不应写入本仓库，避免两个业务模块耦合。
 */
public class DashboardRepository {
    public static final ZoneId MALAYSIA_ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardRepository(@Qualifier("dashboardJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<DashboardDtos.Vehicle> vehicles(DashboardCity city) {
        String sql = """
                SELECT vehicle_id, vehicle_label, license_plate, trip_id, resolved_route_id,
                       route_short_name, route_long_name, static_direction_id, latitude, longitude,
                       COALESCE(derived_speed_kmh, source_speed_raw), vehicle_time, last_seen_time,
                       GREATEST(0, EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP-last_seen_time)))::bigint, qc_flags
                FROM %s.vehicle_latest_state
                WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND spatial_eligible IS TRUE
                ORDER BY vehicle_id
                """.formatted(city.schema());
        return jdbc.query(sql, (rs, rowNum) -> vehicle(city, rs));
    }

    /**
     * Returns one focused active route. The road line comes from its referenced GTFS static shape,
     * while recent observations remain separate point samples. Both reads are bounded and read-only.
     */
    public DashboardDtos.RouteTraces routeTraces(DashboardCity city, String requestedRoute) {
        FocusedRoute route = focusedRoute(city, requestedRoute);
        if (route == null) {
            return new DashboardDtos.RouteTraces(city.code(), Instant.now(), null, null, null,
                    0, 0, 120, List.of(), List.of());
        }
        String sql = """
                WITH active_vehicles AS (
                    SELECT feed_uid, vehicle_id, static_direction_id,
                           row_number() OVER (ORDER BY last_seen_time DESC, vehicle_id) AS vehicle_rank
                    FROM %1$s.vehicle_latest_state
                    WHERE last_seen_time >= CURRENT_TIMESTAMP - interval '10 minutes'
                      AND spatial_eligible IS TRUE
                      AND COALESCE(route_short_name, resolved_route_id) = ?
                ), recent_points AS (
                    SELECT o.vehicle_id, o.static_direction_id, o.latitude, o.longitude, o.vehicle_time,
                           row_number() OVER (
                               PARTITION BY o.feed_uid, o.vehicle_id ORDER BY o.vehicle_time DESC
                           ) AS point_rank
                    FROM active_vehicles av
                    JOIN %1$s.vehicle_observation o
                      ON o.feed_uid = av.feed_uid AND o.vehicle_id = av.vehicle_id
                    WHERE av.vehicle_rank <= 16
                      AND COALESCE(o.route_short_name, o.resolved_route_id) = ?
                      AND o.vehicle_time >= CURRENT_TIMESTAMP - interval '120 minutes'
                      AND o.spatial_eligible IS TRUE
                      AND o.position_wgs84_valid IS TRUE
                      AND COALESCE(o.zero_zero_position, FALSE) IS FALSE
                      AND COALESCE(o.gps_jump_status, '') <> 'GPS_JUMP'
                      AND o.latitude IS NOT NULL AND o.longitude IS NOT NULL
                )
                SELECT vehicle_id, static_direction_id, latitude, longitude, vehicle_time
                FROM recent_points WHERE point_rank <= 90
                ORDER BY vehicle_id, vehicle_time
                """.formatted(city.schema());
        Map<String, TraceBuilder> traces = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            String vehicleId = string(rs, "vehicle_id", "--");
            TraceBuilder trace = traces.get(vehicleId);
            if (trace == null) {
                trace = new TraceBuilder(route.routeKey, route.routeShortName,
                        route.routeLongName, vehicleId,
                        shortValue(rs.getObject("static_direction_id")));
                traces.put(vehicleId, trace);
            }
            trace.points.add(new DashboardDtos.RoutePoint(doubleValue(rs.getObject("latitude")),
                    doubleValue(rs.getObject("longitude")), instant(rs, "vehicle_time")));
        }, route.routeKey, route.routeKey);
        List<DashboardDtos.RouteTrace> traceDtos = traces.values().stream().filter(trace -> !trace.points.isEmpty())
                .map(TraceBuilder::toDto).toList();
        List<DashboardDtos.RouteShape> roadShapes = roadShapes(city, route.routeKey);
        return new DashboardDtos.RouteTraces(city.code(), Instant.now(), route.routeKey,
                route.routeShortName, route.routeLongName, 1, traceDtos.size(), 120,
                roadShapes, traceDtos);
    }

    public DashboardDtos.RouteTraces allRouteTraces(DashboardCity city) {
        List<DashboardDtos.RouteShape> roadShapes = allRoadShapes(city);
        int routeCount = (int) roadShapes.stream().map(DashboardDtos.RouteShape::routeKey).distinct().count();
        return new DashboardDtos.RouteTraces(city.code(), Instant.now(), null, null, null,
                routeCount, 0, 120, roadShapes, List.of());
    }

    private FocusedRoute focusedRoute(DashboardCity city, String requestedRoute) {
        String requested = requestedRoute == null || requestedRoute.isBlank() ? null : requestedRoute.trim();
        String sql = """
                SELECT COALESCE(route_short_name, resolved_route_id) route_key,
                       max(route_short_name) route_short_name, max(route_long_name) route_long_name,
                       count(*) vehicle_count, max(last_seen_time) latest_seen
                FROM %s.vehicle_latest_state
                WHERE last_seen_time >= CURRENT_TIMESTAMP - interval '10 minutes'
                  AND spatial_eligible IS TRUE
                  AND COALESCE(route_short_name, resolved_route_id) IS NOT NULL
                %s
                GROUP BY COALESCE(route_short_name, resolved_route_id)
                ORDER BY vehicle_count DESC, latest_seen DESC, route_key
                LIMIT 1
                """.formatted(city.schema(), requested == null ? "" :
                "AND COALESCE(route_short_name, resolved_route_id) = ?");
        List<FocusedRoute> routes = requested == null
                ? jdbc.query(sql, (rs, n) -> focusedRoute(rs))
                : jdbc.query(sql, (rs, n) -> focusedRoute(rs), requested);
        if (routes.isEmpty() && requested != null) return focusedRoute(city, null);
        return routes.isEmpty() ? null : routes.get(0);
    }

    private FocusedRoute focusedRoute(ResultSet rs) throws SQLException {
        return new FocusedRoute(string(rs, "route_key", "--"),
                string(rs, "route_short_name", null), string(rs, "route_long_name", null));
    }

    private List<DashboardDtos.RouteShape> roadShapes(DashboardCity city, String routeKey) {
        String sql = """
                WITH shape_usage AS (
                    SELECT o.static_shape_uid, COALESCE(o.static_direction_id, -1) direction_id,
                           count(*) usage_count, max(o.vehicle_time) latest_seen
                    FROM %1$s.vehicle_observation o
                    WHERE COALESCE(o.route_short_name, o.resolved_route_id) = ?
                      AND o.vehicle_time >= CURRENT_TIMESTAMP - interval '24 hours'
                      AND o.static_shape_uid IS NOT NULL
                    GROUP BY o.static_shape_uid, COALESCE(o.static_direction_id, -1)
                ), chosen_shapes AS (
                    SELECT *, row_number() OVER (
                        PARTITION BY direction_id ORDER BY usage_count DESC, latest_seen DESC
                    ) shape_rank
                    FROM shape_usage
                )
                SELECT c.direction_id, p.shape_id, p.shape_pt_lat, p.shape_pt_lon
                FROM chosen_shapes c
                
                JOIN %1$s.static_shape_point p ON p.shape_uid = c.static_shape_uid
                WHERE c.shape_rank = 1 AND p.spatial_eligible IS TRUE
                  AND p.shape_pt_lat IS NOT NULL AND p.shape_pt_lon IS NOT NULL
                ORDER BY c.direction_id, p.shape_pt_sequence
                """.formatted(city.schema());
        Map<String, ShapeBuilder> shapes = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            short rawDirection = rs.getShort("direction_id");
            Short directionId = rawDirection < 0 ? null : rawDirection;
            String shapeId = string(rs, "shape_id", "--");
            String key = rawDirection + "\u0000" + shapeId;
            ShapeBuilder shape = shapes.computeIfAbsent(key,
                    ignored -> new ShapeBuilder(routeKey, directionId, shapeId));
            shape.points.add(new DashboardDtos.RoutePoint(doubleValue(rs.getObject("shape_pt_lat")),
                    doubleValue(rs.getObject("shape_pt_lon")), null));
        }, routeKey);
        return shapes.values().stream().filter(shape -> shape.points.size() >= 2)
                .map(ShapeBuilder::toDto).toList();
    }

    private List<DashboardDtos.RouteShape> allRoadShapes(DashboardCity city) {
        String sql = """
                WITH active_routes AS (
                    SELECT DISTINCT COALESCE(route_short_name, resolved_route_id) route_key
                    FROM %1$s.vehicle_latest_state
                    WHERE last_seen_time >= CURRENT_TIMESTAMP - interval '10 minutes'
                      AND spatial_eligible IS TRUE
                      AND COALESCE(route_short_name, resolved_route_id) IS NOT NULL
                ), shape_usage AS (
                    SELECT COALESCE(o.route_short_name, o.resolved_route_id) route_key,
                           o.static_shape_uid, COALESCE(o.static_direction_id, -1) direction_id,
                           count(*) usage_count, max(o.vehicle_time) latest_seen
                    FROM %1$s.vehicle_observation o
                    JOIN active_routes ar
                      ON ar.route_key = COALESCE(o.route_short_name, o.resolved_route_id)
                    WHERE o.vehicle_time >= CURRENT_TIMESTAMP - interval '24 hours'
                      AND o.static_shape_uid IS NOT NULL
                    GROUP BY COALESCE(o.route_short_name, o.resolved_route_id),
                             o.static_shape_uid, COALESCE(o.static_direction_id, -1)
                ), chosen_shapes AS (
                    SELECT *, row_number() OVER (
                        PARTITION BY route_key, direction_id
                        ORDER BY usage_count DESC, latest_seen DESC
                    ) shape_rank
                    FROM shape_usage
                )
                SELECT c.route_key, c.direction_id, p.shape_id, p.shape_pt_lat, p.shape_pt_lon
                FROM chosen_shapes c
                JOIN %1$s.static_shape_point p ON p.shape_uid = c.static_shape_uid
                WHERE c.shape_rank = 1 AND p.spatial_eligible IS TRUE
                  AND p.shape_pt_lat IS NOT NULL AND p.shape_pt_lon IS NOT NULL
                ORDER BY c.route_key, c.direction_id, p.shape_pt_sequence
                """.formatted(city.schema());
        Map<String, ShapeBuilder> shapes = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            String routeKey = string(rs, "route_key", "--");
            short rawDirection = rs.getShort("direction_id");
            Short directionId = rawDirection < 0 ? null : rawDirection;
            String shapeId = string(rs, "shape_id", "--");
            String key = routeKey + "\u0000" + rawDirection + "\u0000" + shapeId;
            ShapeBuilder shape = shapes.computeIfAbsent(key,
                    ignored -> new ShapeBuilder(routeKey, directionId, shapeId));
            shape.points.add(new DashboardDtos.RoutePoint(doubleValue(rs.getObject("shape_pt_lat")),
                    doubleValue(rs.getObject("shape_pt_lon")), null));
        });
        return shapes.values().stream().filter(shape -> shape.points.size() >= 2)
                .map(ShapeBuilder::toDto).toList();
    }

    public DashboardDtos.Acquisition acquisition(DashboardCity city) {
        Instant startToday = LocalDate.now(MALAYSIA_ZONE).atStartOfDay(MALAYSIA_ZONE).toInstant();
        Instant startYesterday = LocalDate.now(MALAYSIA_ZONE).minusDays(1).atStartOfDay(MALAYSIA_ZONE).toInstant();
        String sql = """
                SELECT (SELECT max(request_started_at) FROM %1$s.api_request_log) latest_request,
                       (SELECT result FROM %1$s.api_request_log ORDER BY request_started_at DESC LIMIT 1) api_status,
                       count(*) FILTER (WHERE ingest_time >= ?) today_records,
                       count(*) FILTER (WHERE ingest_time >= ? AND ingest_time < ?) yesterday_records,
                       (SELECT count(*) FROM %1$s.vehicle_observation_qc WHERE create_time >= CURRENT_TIMESTAMP-interval '1 hour') anomalies,
                       (SELECT count(*) FROM %1$s.api_request_log WHERE request_started_at >= CURRENT_TIMESTAMP-interval '24 hours') requests,
                       (SELECT count(*) FROM %1$s.api_request_log WHERE request_started_at >= CURRENT_TIMESTAMP-interval '24 hours'
                          AND http_status BETWEEN 200 AND 299 AND result='SUCCESS') successes
                FROM %1$s.vehicle_observation
                """.formatted(city.schema());
        return jdbc.queryForObject(sql, (rs, n) -> new DashboardDtos.Acquisition(
                city.code(), city.nameEn(), instant(rs, "latest_request"), string(rs, "api_status", "UNKNOWN"),
                rs.getLong("today_records"), rs.getLong("yesterday_records"), rs.getLong("today_records"),
                rs.getLong("anomalies"), rs.getLong("requests"), rs.getLong("successes")),
                Timestamp.from(startToday), Timestamp.from(startYesterday), Timestamp.from(startToday));
    }

    public List<DashboardDtos.TrendPoint> trend(DashboardCity city, LocalDate date) {
        Instant start = date.atStartOfDay(MALAYSIA_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(MALAYSIA_ZONE).toInstant();
        String sql = """
                SELECT to_timestamp(floor(extract(epoch FROM ingest_time)/300)*300) bucket_time, count(*) record_count
                FROM %s.vehicle_observation WHERE ingest_time >= ? AND ingest_time < ?
                GROUP BY bucket_time ORDER BY bucket_time
                """.formatted(city.schema());
        return jdbc.query(sql, (rs, n) -> new DashboardDtos.TrendPoint(city.code(),
                instant(rs, "bucket_time"), rs.getLong("record_count")), Timestamp.from(start), Timestamp.from(end));
    }

    public long latestVehicleCount(DashboardCity city) {
        return jdbc.queryForObject("SELECT count(*) FROM " + city.schema() +
                ".vehicle_latest_state WHERE analysis_eligible IS TRUE AND spatial_eligible IS TRUE " +
                "AND last_seen_time >= CURRENT_TIMESTAMP-interval '10 minutes'", Long.class);
    }

    public long observationCount(DashboardCity city) {
        Long value = jdbc.queryForObject("SELECT count(*) FROM " + city.schema() + ".vehicle_observation", Long.class);
        return value == null ? 0 : value;
    }

    public Instant latestGps(DashboardCity city) {
        return jdbc.queryForObject("SELECT max(last_seen_time) FROM " + city.schema() + ".vehicle_latest_state",
                (rs, n) -> instant(rs, 1));
    }

    public long abnormalVehicleCount(DashboardCity city) {
        String sql = "SELECT count(DISTINCT o.vehicle_id) FROM " + city.schema() + ".vehicle_observation_qc q " +
                "JOIN " + city.schema() + ".vehicle_observation o ON o.uid=q.observation_uid " +
                "WHERE q.create_time >= CURRENT_TIMESTAMP-interval '1 hour'";
        return jdbc.queryForObject(sql, Long.class);
    }

    public List<DashboardDtos.Anomaly> anomalies(DashboardCity city, int limit) {
        String sql = """
                SELECT q.create_time, o.vehicle_id, COALESCE(o.route_short_name,o.resolved_route_id) bus_route,
                       q.qc_code, COALESCE(q.details::text,q.qc_value_text,'--') details, q.severity
                FROM %1$s.vehicle_observation_qc q JOIN %1$s.vehicle_observation o ON o.uid=q.observation_uid
                ORDER BY q.create_time DESC LIMIT ?
                """.formatted(city.schema());
        return jdbc.query(sql, (rs, n) -> new DashboardDtos.Anomaly(instant(rs, "create_time"), city.code(),
                city.nameEn(), string(rs, "vehicle_id", "--"), string(rs, "bus_route", "--"),
                string(rs, "qc_code", "UNKNOWN"), string(rs, "details", "--"),
                string(rs, "severity", "WARNING")), limit);
    }

    public List<DashboardDtos.ApiRequest> apiRequests(DashboardCity city, int limit) {
        String sql = """
                SELECT request_started_at, request_type, http_status, latency_ms, result
                FROM %s.api_request_log ORDER BY request_started_at DESC LIMIT ?
                """.formatted(city.schema());
        return jdbc.query(sql, (rs, n) -> new DashboardDtos.ApiRequest(instant(rs, "request_started_at"),
                city.code(), city.nameEn(), string(rs, "request_type", "--"), (Integer) rs.getObject("http_status"),
                (Long) rs.getObject("latency_ms"), string(rs, "result", "UNKNOWN")), limit);
    }

    private DashboardDtos.Vehicle vehicle(DashboardCity city, ResultSet rs) throws SQLException {
        return new DashboardDtos.Vehicle(city.code(), city.nameEn(), string(rs, "vehicle_id", "--"),
                string(rs, "vehicle_label", null), string(rs, "license_plate", null), string(rs, "trip_id", null),
                string(rs, "resolved_route_id", null), string(rs, "route_short_name", null),
                string(rs, "route_long_name", null), shortValue(rs.getObject("static_direction_id")),
                doubleValue(rs.getObject("latitude")), doubleValue(rs.getObject("longitude")), doubleValue(rs.getObject(11)),
                instant(rs, "vehicle_time"), instant(rs, "last_seen_time"), longValue(rs.getObject(14)), flags(rs.getString("qc_flags")));
    }

    private List<String> flags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> values = new ArrayList<>();
            if (node.isArray()) node.forEach(item -> values.add(item.asText()));
            return List.copyOf(values);
        } catch (Exception ignored) { return List.of(); }
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static Instant instant(ResultSet rs, int column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
    private static String string(ResultSet rs, String column, String fallback) throws SQLException {
        String value = rs.getString(column);
        return value == null || value.isBlank() ? fallback : value;
    }
    private static Short shortValue(Object value) { return value instanceof Number n ? n.shortValue() : null; }
    private static Long longValue(Object value) { return value instanceof Number n ? n.longValue() : null; }
    private static Double doubleValue(Object value) { return value instanceof Number n ? n.doubleValue() : null; }

    /** Dashboard 当前聚焦线路的只读标识与名称。 */
    private record FocusedRoute(String routeKey, String routeShortName, String routeLongName) { }

    /** 按线路、方向和 Shape 聚合 Dashboard 轨迹点。 */
    private static final class ShapeBuilder {
        private final String routeKey;
        private final Short directionId;
        private final String shapeId;
        private final List<DashboardDtos.RoutePoint> points = new ArrayList<>();

        private ShapeBuilder(String routeKey, Short directionId, String shapeId) {
            this.routeKey = routeKey;
            this.directionId = directionId;
            this.shapeId = shapeId;
        }

        private DashboardDtos.RouteShape toDto() {
            return new DashboardDtos.RouteShape(routeKey, directionId, shapeId, List.copyOf(points));
        }
    }

    /** 按车辆聚合 Dashboard 实际运行轨迹点。 */
    private static final class TraceBuilder {
        private final String routeKey;
        private final String routeShortName;
        private final String routeLongName;
        private final String vehicleId;
        private final Short directionId;
        private final List<DashboardDtos.RoutePoint> points = new ArrayList<>();

        private TraceBuilder(String routeKey, String routeShortName, String routeLongName,
                             String vehicleId, Short directionId) {
            this.routeKey = routeKey;
            this.routeShortName = routeShortName;
            this.routeLongName = routeLongName;
            this.vehicleId = vehicleId;
            this.directionId = directionId;
        }

        private DashboardDtos.RouteTrace toDto() {
            return new DashboardDtos.RouteTrace(routeKey, routeShortName, routeLongName,
                    vehicleId, directionId, List.copyOf(points));
        }
    }
}

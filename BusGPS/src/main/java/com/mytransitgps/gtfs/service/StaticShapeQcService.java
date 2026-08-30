package com.mytransitgps.gtfs.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StaticShapeQcResult;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.util.GeoUtils;
import com.mytransitgps.gtfs.util.StatsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static shape 质检服务，检查形状点坐标、边界和线路累计距离。
 */
public class StaticShapeQcService {

    private static final Logger log = LoggerFactory.getLogger(StaticShapeQcService.class);
    private final double maxSegmentKm;

    public StaticShapeQcService() {
        this(10.0d);
    }

    public StaticShapeQcService(double maxSegmentKm) {
        this.maxSegmentKm = maxSegmentKm;
    }

    public StaticShapeQcResult analyze(String feedId, StaticFeedData staticFeedData) {
        // 统计只描述 Static 数据质量，不修改 shape 点或过滤异常路线。
        Set<String> referencedShapeIds = new LinkedHashSet<>();
        Map<String, Integer> tripCountByShape = new LinkedHashMap<>();
        for (TripInfo trip : staticFeedData.trips().values()) {
            if (trip.shapeId() != null && !trip.shapeId().isBlank()) {
                referencedShapeIds.add(trip.shapeId());
                tripCountByShape.merge(trip.shapeId(), 1, Integer::sum);
            }
        }

        List<Map<String, Object>> shapeDetails = new ArrayList<>();
        List<Map<String, Object>> routeShapeLengths = new ArrayList<>();
        int orphanCount = 0;
        int invalidCount = 0;
        int missingReferencedShape = 0;
        int segmentJumpCount = 0;
        List<Double> referencedLengthsKm = new ArrayList<>();

        for (String referencedShapeId : referencedShapeIds) {
            if (!staticFeedData.shapesById().containsKey(referencedShapeId)) {
                missingReferencedShape++;
            }
        }

        for (Map.Entry<String, List<ShapePoint>> entry : staticFeedData.shapesById().entrySet()) {
            String shapeId = entry.getKey();
            List<ShapePoint> points = entry.getValue();
            boolean referenced = referencedShapeIds.contains(shapeId);
            if (!referenced) {
                orphanCount++;
            }
            ShapeAnalysis analysis = analyzeShape(shapeId, points);
            if (analysis.invalid()) {
                invalidCount++;
            }
            segmentJumpCount += analysis.segmentJumpCount();
            if (referenced) {
                referencedLengthsKm.add(analysis.shapeLengthKm());
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", feedId);
            row.put("shape_id", shapeId);
            row.put("shape_point_count", points.size());
            row.put("referenced_status", referenced ? "REFERENCED_SHAPE" : "ORPHAN_SHAPE");
            row.put("shape_length_km", analysis.shapeLengthKm());
            row.put("invalid_wgs84_count", analysis.invalidWgs84Count());
            row.put("zero_zero_count", analysis.zeroZeroCount());
            row.put("duplicate_sequence_count", analysis.duplicateSequenceCount());
            row.put("non_monotonic_sequence_count", analysis.nonMonotonicSequenceCount());
            row.put("duplicate_consecutive_point_count", analysis.duplicateConsecutivePointCount());
            row.put("segment_jump_count", analysis.segmentJumpCount());
            row.put("qc_flags", analysis.flags());
            shapeDetails.add(row);
        }

        Map<RouteShapeKey, Integer> routeShapeTripCounts = new LinkedHashMap<>();
        for (TripInfo trip : staticFeedData.trips().values()) {
            if (trip.shapeId() == null || trip.shapeId().isBlank()) {
                continue;
            }
            RouteInfo route = staticFeedData.routes().get(trip.routeId());
            routeShapeTripCounts.merge(new RouteShapeKey(
                    trip.routeId(),
                    route == null ? null : route.routeShortName(),
                    route == null ? null : route.routeLongName(),
                    trip.directionId(),
                    trip.shapeId()), 1, Integer::sum);
        }
        for (Map.Entry<RouteShapeKey, Integer> entry : routeShapeTripCounts.entrySet()) {
            ShapeAnalysis analysis = analyzeShape(entry.getKey().shapeId, staticFeedData.shapesById().getOrDefault(entry.getKey().shapeId, List.of()));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", feedId);
            row.put("route_id", entry.getKey().routeId);
            row.put("route_short_name", entry.getKey().routeShortName);
            row.put("route_long_name", entry.getKey().routeLongName);
            row.put("direction_id", entry.getKey().directionId);
            row.put("shape_id", entry.getKey().shapeId);
            row.put("shape_length_km", analysis.shapeLengthKm());
            row.put("trip_count_using_shape", entry.getValue());
            routeShapeLengths.add(row);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("feed_id", feedId);
        summary.put("shapes_total", staticFeedData.shapesById().size());
        summary.put("referenced_shape_count", referencedShapeIds.stream().filter(staticFeedData.shapesById()::containsKey).count());
        summary.put("orphan_shape_count", orphanCount);
        summary.put("invalid_shape_count", invalidCount);
        summary.put("missing_referenced_shape_count", missingReferencedShape);
        summary.put("segment_jump_count", segmentJumpCount);
        summary.put("referenced_length_min_km", StatsUtils.minDouble(referencedLengthsKm));
        summary.put("referenced_length_p50_km", StatsUtils.percentileDouble(referencedLengthsKm, 50));
        summary.put("referenced_length_p90_km", StatsUtils.percentileDouble(referencedLengthsKm, 90));
        summary.put("referenced_length_max_km", StatsUtils.maxDouble(referencedLengthsKm));
        log.info("Static Shape质检完成，feedId={}，shapes={}，referenced={}，orphan={}，invalid={}，missingReferenced={}，segmentJumps={}",
                feedId, staticFeedData.shapesById().size(), referencedShapeIds.stream().filter(staticFeedData.shapesById()::containsKey).count(),
                orphanCount, invalidCount, missingReferencedShape, segmentJumpCount);
        if (orphanCount > 0 || invalidCount > 0 || missingReferencedShape > 0 || segmentJumpCount > 0) {
            log.warn("Static Shape存在质量异常，feedId={}，orphan={}，invalid={}，missingReferenced={}，segmentJumps={}，原始Shape继续保留",
                    feedId, orphanCount, invalidCount, missingReferencedShape, segmentJumpCount);
        }

        return new StaticShapeQcResult(summary, shapeDetails, routeShapeLengths);
    }

    private ShapeAnalysis analyzeShape(String shapeId, List<ShapePoint> points) {
        int invalidWgs84Count = 0;
        int zeroZeroCount = 0;
        int duplicateSequenceCount = 0;
        int nonMonotonicSequenceCount = 0;
        int duplicateConsecutivePointCount = 0;
        int segmentJumpCount = 0;
        Set<Integer> seenSequences = new LinkedHashSet<>();
        List<String> flags = new ArrayList<>();
        double lengthMeters = 0.0d;
        ShapePoint previous = null;
        Integer previousSequence = null;

        for (ShapePoint point : points) {
            if (shapeId == null || shapeId.isBlank()) {
                flags.add("MISSING_SHAPE_ID");
            }
            if (point.sequence() != null && !seenSequences.add(point.sequence())) {
                duplicateSequenceCount++;
            }
            if (previousSequence != null && point.sequence() != null && point.sequence() < previousSequence) {
                nonMonotonicSequenceCount++;
            }
            if (!validWgs84(point.latitude(), point.longitude())) {
                invalidWgs84Count++;
            } else if (Double.compare(point.latitude(), 0.0d) == 0 && Double.compare(point.longitude(), 0.0d) == 0) {
                zeroZeroCount++;
            }
            if (previous != null && validWgs84(previous.latitude(), previous.longitude()) && validWgs84(point.latitude(), point.longitude())) {
                double segmentMeters = GeoUtils.haversineMeters(previous.latitude(), previous.longitude(), point.latitude(), point.longitude());
                lengthMeters += segmentMeters;
                if (Double.compare(previous.latitude(), point.latitude()) == 0 && Double.compare(previous.longitude(), point.longitude()) == 0) {
                    duplicateConsecutivePointCount++;
                }
                if ((segmentMeters / 1000.0d) > maxSegmentKm) {
                    segmentJumpCount++;
                }
            }
            previous = point;
            previousSequence = point.sequence();
        }

        if (invalidWgs84Count > 0) {
            flags.add("INVALID_WGS84");
        }
        if (zeroZeroCount > 0) {
            flags.add("ZERO_ZERO_POINT");
        }
        if (duplicateSequenceCount > 0) {
            flags.add("DUPLICATE_SEQUENCE");
        }
        if (nonMonotonicSequenceCount > 0) {
            flags.add("NON_MONOTONIC_SEQUENCE");
        }
        if (duplicateConsecutivePointCount > 0) {
            flags.add("DUPLICATE_CONSECUTIVE_POINTS");
        }
        if (segmentJumpCount > 0) {
            flags.add("SHAPE_SEGMENT_JUMP");
        }

        return new ShapeAnalysis(
                !flags.isEmpty(),
                invalidWgs84Count,
                zeroZeroCount,
                duplicateSequenceCount,
                nonMonotonicSequenceCount,
                duplicateConsecutivePointCount,
                segmentJumpCount,
                lengthMeters / 1000.0d,
                flags.isEmpty() ? List.of("VALID_SHAPE") : List.copyOf(flags));
    }

    private boolean validWgs84(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && !latitude.isNaN()
                && !longitude.isNaN()
                && !latitude.isInfinite()
                && !longitude.isInfinite()
                && latitude >= -90.0d
                && latitude <= 90.0d
                && longitude >= -180.0d
                && longitude <= 180.0d;
    }

    /** 路线、方向和 shape 组合的质检汇总键。 */
    private record RouteShapeKey(
            String routeId,
            String routeShortName,
            String routeLongName,
            Integer directionId,
            String shapeId) {
    }

    /** 单条 shape 的点数、距离和异常状态分析结果。 */
    private record ShapeAnalysis(
            boolean invalid,
            int invalidWgs84Count,
            int zeroZeroCount,
            int duplicateSequenceCount,
            int nonMonotonicSequenceCount,
            int duplicateConsecutivePointCount,
            int segmentJumpCount,
            double shapeLengthKm,
            List<String> flags) {
    }
}

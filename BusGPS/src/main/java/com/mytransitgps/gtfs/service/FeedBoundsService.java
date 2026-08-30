package com.mytransitgps.gtfs.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.ShapePoint;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.StopInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Feed 空间边界服务，根据 Static 站点范围生成带缓冲区的合法坐标范围。
 */
public class FeedBoundsService {

    private static final Logger log = LoggerFactory.getLogger(FeedBoundsService.class);

    public static final String REFERENCED_SHAPES_PLUS_STOPS = "REFERENCED_SHAPES_PLUS_STOPS";
    public static final String STOPS_ONLY = "STOPS_ONLY";
    public static final String BOUNDS_UNAVAILABLE = "BOUNDS_UNAVAILABLE";
    public static final String NO_REFERENCED_SHAPE_POINTS = "NO_REFERENCED_SHAPE_POINTS";

    private final double bufferKm;

    public FeedBoundsService() {
        this(10.0d);
    }

    public FeedBoundsService(double bufferKm) {
        this.bufferKm = bufferKm;
    }

    public FeedSpatialBounds compute(String feedId, String staticSha256, StaticFeedData staticFeedData) {
        // 优先使用被 trip 引用的 shape 点估计运营范围，避免孤立 shape 扩大边界。
        Set<String> referencedShapeIds = new LinkedHashSet<>();
        for (TripInfo trip : staticFeedData.trips().values()) {
            if (trip.shapeId() != null && !trip.shapeId().isBlank()) {
                referencedShapeIds.add(trip.shapeId());
            }
        }

        List<Double> latitudes = new ArrayList<>();
        List<Double> longitudes = new ArrayList<>();
        int referencedShapePointCount = 0;
        int stopCount = 0;

        for (String shapeId : referencedShapeIds) {
            List<ShapePoint> shapePoints = staticFeedData.shapesById().get(shapeId);
            if (shapePoints == null) {
                continue;
            }
            for (ShapePoint shapePoint : shapePoints) {
                if (shapePoint.latitude() != null && shapePoint.longitude() != null) {
                    latitudes.add(shapePoint.latitude());
                    longitudes.add(shapePoint.longitude());
                    referencedShapePointCount++;
                }
            }
        }
        for (StopInfo stop : staticFeedData.stops().values()) {
            if (stop.stopLat() != null && stop.stopLon() != null) {
                latitudes.add(stop.stopLat());
                longitudes.add(stop.stopLon());
                stopCount++;
            }
        }

        long referencedShapeCount = referencedShapeIds.stream()
                .filter(staticFeedData.shapesById()::containsKey)
                .count();
        int totalShapeCount = staticFeedData.shapesById().size();
        int orphanShapeCount = totalShapeCount - (int) referencedShapeCount;

        if (referencedShapePointCount == 0 && stopCount == 0) {
            log.warn("Feed空间边界不可用，feedId={}，referencedShapes={}，orphanShapes={}，stops={}，staticSha256={}",
                    feedId, referencedShapeCount, orphanShapeCount, stopCount, staticSha256);
            return new FeedSpatialBounds(
                    feedId,
                    totalShapeCount,
                    (int) referencedShapeCount,
                    orphanShapeCount,
                    referencedShapePointCount,
                    stopCount,
                    BOUNDS_UNAVAILABLE,
                    NO_REFERENCED_SHAPE_POINTS,
                    staticSha256,
                    null,
                    null,
                    null,
                    null,
                    bufferKm,
                    null,
                    null,
                    null,
                    null);
        }

        double rawMinLat = latitudes.stream().min(Double::compareTo).orElse(-90.0d);
        double rawMaxLat = latitudes.stream().max(Double::compareTo).orElse(90.0d);
        double rawMinLon = longitudes.stream().min(Double::compareTo).orElse(-180.0d);
        double rawMaxLon = longitudes.stream().max(Double::compareTo).orElse(180.0d);

        double centerLat = (rawMinLat + rawMaxLat) / 2.0d;
        double latBufferDegrees = bufferKm / 111.32d;
        double lonDenominator = 111.32d * Math.max(0.1d, Math.cos(Math.toRadians(centerLat)));
        double lonBufferDegrees = bufferKm / lonDenominator;

        String boundsSource = referencedShapePointCount > 0 ? REFERENCED_SHAPES_PLUS_STOPS : STOPS_ONLY;
        String boundsWarning = referencedShapePointCount > 0 ? null : NO_REFERENCED_SHAPE_POINTS;
        if (STOPS_ONLY.equals(boundsSource)) {
            log.warn("Feed空间边界降级为STOPS_ONLY，feedId={}，stops={}，referencedShapePoints=0，bufferKm={}",
                    feedId, stopCount, bufferKm);
        }
        log.info("Feed空间边界计算完成，feedId={}，source={}，referencedShapes={}，orphanShapes={}，shapePoints={}，stops={}，rawLat=[{},{}]，rawLon=[{},{}]，bufferKm={}",
                feedId, boundsSource, referencedShapeCount, orphanShapeCount, referencedShapePointCount, stopCount,
                rawMinLat, rawMaxLat, rawMinLon, rawMaxLon, bufferKm);

        return new FeedSpatialBounds(
                feedId,
                totalShapeCount,
                (int) referencedShapeCount,
                orphanShapeCount,
                referencedShapePointCount,
                stopCount,
                boundsSource,
                boundsWarning,
                staticSha256,
                rawMinLat,
                rawMaxLat,
                rawMinLon,
                rawMaxLon,
                bufferKm,
                rawMinLat - latBufferDegrees,
                rawMaxLat + latBufferDegrees,
                rawMinLon - lonBufferDegrees,
                rawMaxLon + lonBufferDegrees);
    }
}

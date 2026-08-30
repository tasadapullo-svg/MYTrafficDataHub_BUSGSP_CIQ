package com.mytransitgps.gtfs.model;

import java.util.List;
import java.util.Map;

/**
 * 解析后的 Static GTFS 数据集合，集中保存路线、班次、站点和形状索引。
 */
public record StaticFeedData(
        Map<String, RouteInfo> routes,
        Map<String, TripInfo> trips,
        Map<String, StopInfo> stops,
        Map<String, List<StopTimeInfo>> stopTimesByTrip,
        Map<String, List<ShapePoint>> shapesById,
        int stopsCount,
        int stopTimesCount,
        boolean routesPresent,
        boolean tripsPresent,
        boolean stopsPresent,
        boolean stopTimesPresent,
        boolean shapesPresent,
        boolean calendarPresent,
        boolean calendarDatesPresent,
        boolean frequenciesPresent) {

    public StaticFeedData(
            Map<String, RouteInfo> routes,
            Map<String, TripInfo> trips,
            int stopsCount,
            int stopTimesCount,
            boolean routesPresent,
            boolean tripsPresent,
            boolean stopsPresent,
            boolean stopTimesPresent,
            boolean shapesPresent,
            boolean calendarPresent,
            boolean calendarDatesPresent,
            boolean frequenciesPresent) {
        this(
                routes,
                trips,
                Map.of(),
                Map.of(),
                Map.of(),
                stopsCount,
                stopTimesCount,
                routesPresent,
                tripsPresent,
                stopsPresent,
                stopTimesPresent,
                shapesPresent,
                calendarPresent,
                calendarDatesPresent,
                frequenciesPresent);
    }
}

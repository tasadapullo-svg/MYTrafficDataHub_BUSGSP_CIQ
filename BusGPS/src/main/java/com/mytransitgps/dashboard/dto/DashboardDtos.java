package com.mytransitgps.dashboard.dto;

import java.time.Instant;
import java.util.List;

/**
 * BUS GPS 大屏 REST 接口使用的只读 DTO 集合。
 *
 * <p>集中定义现有前端所需的城市、车辆、轨迹、采集、异常、请求和系统状态数据结构，
 * 不承载数据库实体或采集业务逻辑。
 */
public final class DashboardDtos {
    private DashboardDtos() { }

    /** 大屏城市基础信息。 */
    public record City(String code, String nameEn, String nameZh, double latitude, double longitude, int zoom) { }

    /** 单辆公交车最新展示信息。 */
    public record Vehicle(String cityCode, String cityName, String vehicleId, String vehicleLabel,
                          String licensePlate, String tripId, String resolvedRouteId, String routeShortName,
                          String routeLongName, Short directionId, Double latitude, Double longitude,
                          Double speedKmh, Instant vehicleTime, Instant lastSeenTime, Long freshnessSeconds,
                          List<String> qcFlags) { }

    /** 某城市车辆集合及生成时间。 */
    public record Vehicles(String cityCode, Instant generatedAt, int count, List<Vehicle> vehicles) { }

    /** 路线轨迹中的单个时空点。 */
    public record RoutePoint(Double latitude, Double longitude, Instant vehicleTime) { }

    /** 静态道路 Shape 轨迹。 */
    public record RouteShape(String routeKey, Short directionId, String shapeId, List<RoutePoint> points) { }

    /** 单车历史运行轨迹。 */
    public record RouteTrace(String routeKey, String routeShortName, String routeLongName,
                             String vehicleId, Short directionId, List<RoutePoint> points) { }

    /** 路线 Shape 与车辆历史轨迹的组合响应。 */
    public record RouteTraces(String cityCode, Instant generatedAt, String selectedRouteKey,
                              String selectedRouteShortName, String selectedRouteLongName,
                              int routeCount, int segmentCount, int historyWindowMinutes,
                              List<RouteShape> roadShapes, List<RouteTrace> traces) { }

    /** 单城市采集运行摘要。 */
    public record Acquisition(String cityCode, String cityName, Instant latestRequest, String apiStatus,
                              long todayRecords, long yesterdayRecords, long insertedRecords,
                              long anomalies, long requestCount, long successfulRequests) { }

    /** 按时间桶聚合的数据量趋势点。 */
    public record TrendPoint(String cityCode, Instant bucketTime, long recordCount) { }

    /** 最近异常事件展示记录。 */
    public record Anomaly(Instant time, String cityCode, String cityName, String vehicleId,
                          String busRoute, String anomalyType, String details, String status) { }

    /** 最近外部 API 请求证据。 */
    public record ApiRequest(Instant time, String cityCode, String cityName, String api,
                             Integer httpStatus, Long responseTimeMs, String status) { }

    /** BUS GPS 大屏总览指标。 */
    public record Overview(long activeVehicles, long todayRecords, long yesterdayRecords,
                           long totalRecords, boolean totalRecordsEstimated, Instant latestGpsUpdate,
                           long abnormalVehicles, Double apiSuccessRate, String activeVehiclesDefinition,
                           String abnormalVehiclesDefinition, Instant generatedAt) { }

    /** 数据库和最新 GPS 更新状态。 */
    public record SystemStatus(String status, boolean databaseAvailable, Instant latestGpsUpdate,
                               long latestDataAgeSeconds, String message, Instant checkedAt) { }

    /** 对可能不可用的数据区块提供显式可用性说明。 */
    public record Availability<T>(boolean available, T data, String message) { }
}

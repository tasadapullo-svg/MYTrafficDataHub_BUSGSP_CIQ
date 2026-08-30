package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.util.JsonFormat;
import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.gtfs.config.GtfsFeedDefinition;
import com.mytransitgps.gtfs.model.StaticFeedData;
import com.mytransitgps.gtfs.model.RouteInfo;
import com.mytransitgps.gtfs.model.TripInfo;
import com.mytransitgps.gtfs.util.ProtoFieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Realtime 与 Static GTFS 补全服务，为车辆实体解析路线、班次、方向和形状信息。
 */
public class RealtimeStaticEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeStaticEnrichmentService.class);
    private final JsonOutputService jsonOutputService;

    public RealtimeStaticEnrichmentService(JsonOutputService jsonOutputService) {
        this.jsonOutputService = jsonOutputService;
    }

    public EnrichmentResult enrich(
            String batchId,
            GtfsFeedDefinition feed,
            GtfsRealtime.FeedMessage feedMessage,
            StaticFeedData staticFeedData,
            GtfsHttpResult realtimeHttpResult) throws IOException {

        // 补全只增加派生字段，完整的原始 realtime_entity 仍随每条车辆记录保留。
        log.info("开始Realtime Static补全，batchId={}，城市={}，feedId={}，entityCount={}",
                batchId, feed.cityName(), feed.feedId(), feedMessage.getEntityCount());
        List<Map<String, Object>> vehicles = new ArrayList<>();
        Set<String> uniqueRouteIds = new LinkedHashSet<>();
        int routeDirectMatchCount = 0;
        int routeTripFallbackMatchCount = 0;
        int routeResolvedCount = 0;
        int routeUnresolvedCount = 0;
        int tripMatched = 0;
        int directionMatched = 0;
        int directionMismatched = 0;
        int directionNotComparable = 0;

        for (GtfsRealtime.FeedEntity entity : feedMessage.getEntityList()) {
            if (!entity.hasVehicle()) {
                continue;
            }

            Map<String, Object> vehiclePayload = buildVehiclePayload(entity, staticFeedData);
            vehicles.add(vehiclePayload);

            if (Boolean.TRUE.equals(vehiclePayload.get("realtime_route_direct_matched"))) {
                routeDirectMatchCount++;
            }
            if ("TRIP_TO_STATIC_ROUTE".equals(vehiclePayload.get("route_resolution_method"))) {
                routeTripFallbackMatchCount++;
            }
            if (Boolean.TRUE.equals(vehiclePayload.get("route_resolved"))) {
                routeResolvedCount++;
            } else {
                routeUnresolvedCount++;
            }
            if (Boolean.TRUE.equals(vehiclePayload.get("trip_static_matched"))) {
                tripMatched++;
            }
            if ("MATCH".equals(vehiclePayload.get("direction_comparison"))) {
                directionMatched++;
            } else if ("MISMATCH".equals(vehiclePayload.get("direction_comparison"))) {
                directionMismatched++;
            } else {
                directionNotComparable++;
            }

            String routeId = (String) vehiclePayload.get("resolved_route_id");
            if (routeId != null && !routeId.isBlank()) {
                uniqueRouteIds.add(routeId);
            }
        }

        vehicles.sort(Comparator
                .comparing((Map<String, Object> vehicle) -> (String) vehicle.get("vehicle_id"), Comparator.nullsLast(String::compareTo))
                .thenComparing(vehicle -> (String) vehicle.get("realtime_route_id"), Comparator.nullsLast(String::compareTo)));

        Map<String, Long> activeVehiclesByRoute = new LinkedHashMap<>();
        for (Map<String, Object> vehicle : vehicles) {
            String routeShortName = (String) vehicle.get("route_short_name");
            String routeLongName = (String) vehicle.get("route_long_name");
            String label = routeShortName != null && !routeShortName.isBlank()
                    ? routeShortName
                    : (routeLongName != null && !routeLongName.isBlank() ? routeLongName : "NOT_PROVIDED");
            activeVehiclesByRoute.merge(label, 1L, Long::sum);
        }

        int vehicleCount = vehicles.size();
        int tripUnmatched = vehicleCount - tripMatched;

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("snapshot", buildSnapshot(
                batchId,
                feed,
                feedMessage,
                realtimeHttpResult,
                vehicleCount,
                uniqueRouteIds.size(),
                routeDirectMatchCount,
                routeTripFallbackMatchCount,
                routeResolvedCount,
                routeUnresolvedCount,
                tripMatched,
                tripUnmatched,
                directionMatched,
                directionMismatched,
                directionNotComparable));
        root.put("vehicles", vehicles);
        log.info("Realtime Static补全完成，城市={}，feedId={}，vehicles={}，uniqueRoutes={}，routeResolved={}，routeUnresolved={}，tripMatched={}，tripUnmatched={}，directionMatch={}，directionMismatch={}",
                feed.cityName(), feed.feedId(), vehicleCount, uniqueRouteIds.size(), routeResolvedCount, routeUnresolvedCount,
                tripMatched, tripUnmatched, directionMatched, directionMismatched);
        if (routeUnresolvedCount > 0 || tripUnmatched > 0) {
            log.warn("Realtime Static补全存在未匹配项，城市={}，feedId={}，routeUnresolved={}，tripUnmatched={}，原始Realtime实体继续保留",
                    feed.cityName(), feed.feedId(), routeUnresolvedCount, tripUnmatched);
        }

        return new EnrichmentResult(
                root,
                vehicles,
                vehicleCount,
                uniqueRouteIds.size(),
                routeDirectMatchCount,
                routeTripFallbackMatchCount,
                routeResolvedCount,
                routeUnresolvedCount,
                tripMatched,
                tripUnmatched,
                directionMatched,
                directionMismatched,
                directionNotComparable,
                activeVehiclesByRoute);
    }

    private Map<String, Object> buildSnapshot(
            String batchId,
            GtfsFeedDefinition feed,
            GtfsRealtime.FeedMessage feedMessage,
            GtfsHttpResult realtimeHttpResult,
            int vehicleCount,
            int uniqueRouteCount,
            int routeDirectMatchCount,
            int routeTripFallbackMatchCount,
            int routeResolvedCount,
            int routeUnresolvedCount,
            int tripMatched,
            int tripUnmatched,
            int directionMatched,
            int directionMismatched,
            int directionNotComparable) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("batch_id", batchId);
        snapshot.put("city_code", feed.cityCode());
        snapshot.put("city_name", feed.cityName());
        snapshot.put("feed_id", feed.feedId());
        snapshot.put("operator", feed.operator());
        snapshot.put("service_type", feed.serviceType());
        snapshot.put("gtfs_realtime_version", feedMessage.getHeader().getGtfsRealtimeVersion());
        snapshot.put("incrementality", feedMessage.getHeader().getIncrementality().name());
        snapshot.put("feed_timestamp", feedMessage.getHeader().hasTimestamp() ? feedMessage.getHeader().getTimestamp() : null);
        snapshot.put("feed_timestamp_utc", feedMessage.getHeader().hasTimestamp()
                ? Instant.ofEpochSecond(feedMessage.getHeader().getTimestamp()).toString()
                : null);
        snapshot.put("request_started_at", realtimeHttpResult.requestStartedAt().toString());
        snapshot.put("response_received_at", realtimeHttpResult.responseReceivedAt().toString());
        snapshot.put("ingest_timestamp_utc", realtimeHttpResult.ingestTimestampUtc().toString());
        snapshot.put("entity_count", feedMessage.getEntityCount());
        snapshot.put("vehicle_count", vehicleCount);
        snapshot.put("unique_route_count", uniqueRouteCount);
        snapshot.put("route_direct_match_count", routeDirectMatchCount);
        snapshot.put("route_trip_fallback_match_count", routeTripFallbackMatchCount);
        snapshot.put("route_resolved_count", routeResolvedCount);
        snapshot.put("route_unresolved_count", routeUnresolvedCount);
        snapshot.put("route_resolved_rate", percentage(routeResolvedCount, vehicleCount));
        snapshot.put("trip_static_match_count", tripMatched);
        snapshot.put("trip_static_unmatched_count", tripUnmatched);
        snapshot.put("trip_static_match_rate", percentage(tripMatched, vehicleCount));
        snapshot.put("direction_match_count", directionMatched);
        snapshot.put("direction_mismatch_count", directionMismatched);
        snapshot.put("direction_not_comparable_count", directionNotComparable);
        snapshot.put("response_bytes", realtimeHttpResult.responseBytes());
        snapshot.put("response_sha256", realtimeHttpResult.responseSha256());
        return snapshot;
    }

    private Map<String, Object> buildVehiclePayload(GtfsRealtime.FeedEntity entity, StaticFeedData staticFeedData) throws IOException {
        GtfsRealtime.VehiclePosition vehicle = entity.getVehicle();
        GtfsRealtime.TripDescriptor trip = vehicle.hasTrip() ? vehicle.getTrip() : GtfsRealtime.TripDescriptor.getDefaultInstance();
        GtfsRealtime.VehicleDescriptor descriptor = vehicle.hasVehicle() ? vehicle.getVehicle() : GtfsRealtime.VehicleDescriptor.getDefaultInstance();
        GtfsRealtime.Position position = vehicle.hasPosition() ? vehicle.getPosition() : GtfsRealtime.Position.getDefaultInstance();

        String routeId = textOrNull(trip.hasRouteId(), trip.getRouteId());
        String tripId = textOrNull(trip.hasTripId(), trip.getTripId());
        TripInfo tripInfo = tripId == null ? null : staticFeedData.trips().get(tripId);
        RouteResolution routeResolution = resolveRoute(routeId, tripInfo, staticFeedData);
        RouteInfo routeInfo = routeResolution.routeInfo();
        Integer realtimeDirection = trip.hasDirectionId() ? trip.getDirectionId() : null;
        Integer staticDirection = tripInfo == null ? null : tripInfo.directionId();
        String directionComparison = directionComparison(realtimeDirection, staticDirection);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entity_id", textOrNull(entity.hasId(), entity.getId()));
        payload.put("vehicle_id", textOrNull(descriptor.hasId(), descriptor.getId()));
        payload.put("vehicle_label", textOrNull(descriptor.hasLabel(), descriptor.getLabel()));
        payload.put("license_plate", textOrNull(descriptor.hasLicensePlate(), descriptor.getLicensePlate()));
        payload.put("wheelchair_accessible", ProtoFieldAccess.getEnumName(descriptor, "wheelchair_accessible"));
        payload.put("trip_id", tripId);
        payload.put("trip_start_time", textOrNull(trip.hasStartTime(), trip.getStartTime()));
        payload.put("trip_start_date", textOrNull(trip.hasStartDate(), trip.getStartDate()));
        payload.put("realtime_route_id", routeId);
        payload.put("static_route_id", tripInfo == null ? null : tripInfo.routeId());
        payload.put("resolved_route_id", routeResolution.resolvedRouteId());
        payload.put("realtime_direction_id", realtimeDirection);
        payload.put("realtime_schedule_relationship", trip.hasScheduleRelationship() ? trip.getScheduleRelationship().name() : null);
        payload.put("static_service_id", tripInfo == null ? null : tripInfo.serviceId());
        payload.put("static_direction_id", staticDirection);
        payload.put("direction_comparison", directionComparison);
        payload.put("trip_headsign", tripInfo == null ? null : tripInfo.tripHeadsign());
        payload.put("trip_short_name", tripInfo == null ? null : tripInfo.tripShortName());
        payload.put("shape_id", tripInfo == null ? null : tripInfo.shapeId());
        payload.put("route_short_name", routeInfo == null ? null : routeInfo.routeShortName());
        payload.put("route_long_name", routeInfo == null ? null : routeInfo.routeLongName());
        payload.put("route_type", routeInfo == null ? null : routeInfo.routeType());
        payload.put("route_color", routeInfo == null ? null : routeInfo.routeColor());
        payload.put("route_text_color", routeInfo == null ? null : routeInfo.routeTextColor());
        payload.put("latitude", position.hasLatitude() ? position.getLatitude() : null);
        payload.put("longitude", position.hasLongitude() ? position.getLongitude() : null);
        payload.put("bearing", position.hasBearing() ? position.getBearing() : null);
        payload.put("odometer_m", position.hasOdometer() ? position.getOdometer() : null);
        payload.put("source_speed_raw", position.hasSpeed() ? position.getSpeed() : null);
        payload.put("source_speed_present", position.hasSpeed());
        payload.put("source_speed_unit_declared", "GTFS_SPEC_MPS");
        payload.put("source_speed_unit_interpretation", "UNVERIFIED_PROVIDER_SEMANTICS");
        payload.put("current_stop_sequence", vehicle.hasCurrentStopSequence() ? vehicle.getCurrentStopSequence() : null);
        payload.put("stop_id", textOrNull(vehicle.hasStopId(), vehicle.getStopId()));
        payload.put("current_status", vehicle.hasCurrentStatus() ? vehicle.getCurrentStatus().name() : null);
        payload.put("vehicle_timestamp", vehicle.hasTimestamp() ? vehicle.getTimestamp() : null);
        payload.put("vehicle_timestamp_utc", vehicle.hasTimestamp() ? Instant.ofEpochSecond(vehicle.getTimestamp()).toString() : null);
        payload.put("congestion_level", vehicle.hasCongestionLevel() ? vehicle.getCongestionLevel().name() : null);
        payload.put("occupancy_status", vehicle.hasOccupancyStatus() ? vehicle.getOccupancyStatus().name() : null);
        payload.put("occupancy_percentage", vehicle.hasOccupancyPercentage() ? vehicle.getOccupancyPercentage() : null);
        payload.put("multi_carriage_details", toJsonNodeList(vehicle.getMultiCarriageDetailsList()));
        payload.put("realtime_route_direct_matched", routeResolution.directRealtimeRouteMatched());
        payload.put("route_resolved", routeResolution.routeResolved());
        payload.put("route_resolution_method", routeResolution.method());
        payload.put("route_static_matched", routeResolution.routeResolved());
        payload.put("trip_static_matched", tripInfo != null);
        payload.put("realtime_entity", toJsonNode(entity));
        return payload;
    }

    private RouteResolution resolveRoute(String realtimeRouteId, TripInfo tripInfo, StaticFeedData staticFeedData) {
        // 路线优先使用 Realtime 直接匹配，失败后才通过 Static trip 回退解析。
        RouteInfo directRoute = realtimeRouteId == null ? null : staticFeedData.routes().get(realtimeRouteId);
        if (directRoute != null) {
            return new RouteResolution(true, true, "DIRECT_REALTIME_ROUTE", realtimeRouteId, directRoute);
        }

        String staticRouteId = tripInfo == null ? null : tripInfo.routeId();
        RouteInfo fallbackRoute = staticRouteId == null ? null : staticFeedData.routes().get(staticRouteId);
        if (fallbackRoute != null) {
            return new RouteResolution(false, true, "TRIP_TO_STATIC_ROUTE", staticRouteId, fallbackRoute);
        }

        return new RouteResolution(false, false, "UNRESOLVED", staticRouteId, null);
    }

    private String directionComparison(Integer realtimeDirection, Integer staticDirection) {
        if (realtimeDirection != null && staticDirection != null) {
            return realtimeDirection.equals(staticDirection) ? "MATCH" : "MISMATCH";
        }
        return "NOT_COMPARABLE";
    }

    private List<JsonNode> toJsonNodeList(List<? extends com.google.protobuf.MessageOrBuilder> protobufMessages) throws IOException {
        List<JsonNode> nodes = new ArrayList<>();
        for (com.google.protobuf.MessageOrBuilder protobufMessage : protobufMessages) {
            String json = JsonFormat.printer().preservingProtoFieldNames().print(protobufMessage);
            nodes.add(jsonOutputService.objectMapper().readTree(json));
        }
        return nodes;
    }

    private JsonNode toJsonNode(com.google.protobuf.MessageOrBuilder protobufMessage) throws IOException {
        String json = JsonFormat.printer().preservingProtoFieldNames().print(protobufMessage);
        return jsonOutputService.objectMapper().readTree(json);
    }

    private String textOrNull(boolean present, String value) {
        if (!present || value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String percentage(int matched, int total) {
        if (total == 0) {
            return "0.00%";
        }
        return String.format(java.util.Locale.ROOT, "%.2f%%", (matched * 100.0d) / total);
    }

    /** Realtime 补全后的根对象、车辆集合和关联命中统计。 */
    public record EnrichmentResult(
            Map<String, Object> root,
            List<Map<String, Object>> vehicles,
            int vehicleCount,
            int uniqueRouteCount,
            int routeDirectMatchCount,
            int routeTripFallbackMatchCount,
            int routeResolvedCount,
            int routeUnresolvedCount,
            int tripMatchCount,
            int tripUnmatchedCount,
            int directionMatchCount,
            int directionMismatchCount,
            int directionNotComparableCount,
            Map<String, Long> activeVehiclesByRoute) {
    }

    /** 路线解析结果，明确区分直接匹配、trip 回退和未解析。 */
    private record RouteResolution(
            boolean directRealtimeRouteMatched,
            boolean routeResolved,
            String method,
            String resolvedRouteId,
            RouteInfo routeInfo) {
    }
}

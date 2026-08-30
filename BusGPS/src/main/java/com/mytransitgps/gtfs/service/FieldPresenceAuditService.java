package com.mytransitgps.gtfs.service;

import java.util.List;
import java.util.function.Predicate;

import com.google.transit.realtime.GtfsRealtime;
import com.mytransitgps.gtfs.model.FieldPresenceAudit;
import com.mytransitgps.gtfs.util.ProtoFieldAccess;

/**
 * Realtime 字段存在性审计服务，统计关键字段真实出现频率。
 */
public class FieldPresenceAuditService {

    public FieldPresenceAudit audit(String feedId, GtfsRealtime.FeedMessage feedMessage) {
        List<GtfsRealtime.FeedEntity> vehicles = feedMessage.getEntityList().stream()
                .filter(GtfsRealtime.FeedEntity::hasVehicle)
                .toList();

        int total = vehicles.size();
        FieldPresenceAudit.Builder builder = new FieldPresenceAudit.Builder(feedId, total);
        builder.put("vehicle_id", count(vehicles, entity -> entity.getVehicle().hasVehicle() && entity.getVehicle().getVehicle().hasId()));
        builder.put("vehicle_label", count(vehicles, entity -> entity.getVehicle().hasVehicle() && entity.getVehicle().getVehicle().hasLabel()));
        builder.put("license_plate", count(vehicles, entity -> entity.getVehicle().hasVehicle() && entity.getVehicle().getVehicle().hasLicensePlate()));
        builder.put("wheelchair_accessible", count(vehicles, entity -> entity.getVehicle().hasVehicle()
                && ProtoFieldAccess.hasField(entity.getVehicle().getVehicle(), "wheelchair_accessible")));
        builder.put("trip_id", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasTripId()));
        builder.put("route_id", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasRouteId()));
        builder.put("direction_id", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasDirectionId()));
        builder.put("start_time", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasStartTime()));
        builder.put("start_date", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasStartDate()));
        builder.put("schedule_relationship", count(vehicles, entity -> entity.getVehicle().hasTrip() && entity.getVehicle().getTrip().hasScheduleRelationship()));
        builder.put("position", count(vehicles, entity -> entity.getVehicle().hasPosition()));
        builder.put("latitude", count(vehicles, entity -> entity.getVehicle().hasPosition() && entity.getVehicle().getPosition().hasLatitude()));
        builder.put("longitude", count(vehicles, entity -> entity.getVehicle().hasPosition() && entity.getVehicle().getPosition().hasLongitude()));
        builder.put("speed", count(vehicles, entity -> entity.getVehicle().hasPosition() && entity.getVehicle().getPosition().hasSpeed()));
        builder.put("bearing", count(vehicles, entity -> entity.getVehicle().hasPosition() && entity.getVehicle().getPosition().hasBearing()));
        builder.put("odometer", count(vehicles, entity -> entity.getVehicle().hasPosition() && entity.getVehicle().getPosition().hasOdometer()));
        builder.put("vehicle_timestamp", count(vehicles, entity -> entity.getVehicle().hasTimestamp()));
        builder.put("current_stop_sequence", count(vehicles, entity -> entity.getVehicle().hasCurrentStopSequence()));
        builder.put("stop_id", count(vehicles, entity -> entity.getVehicle().hasStopId()));
        builder.put("current_status", count(vehicles, entity -> entity.getVehicle().hasCurrentStatus()));
        builder.put("congestion_level", count(vehicles, entity -> entity.getVehicle().hasCongestionLevel()));
        builder.put("occupancy_status", count(vehicles, entity -> entity.getVehicle().hasOccupancyStatus()));
        builder.put("occupancy_percentage", count(vehicles, entity -> entity.getVehicle().hasOccupancyPercentage()));
        builder.put("multi_carriage_details", count(vehicles, entity -> entity.getVehicle().getMultiCarriageDetailsCount() > 0));
        return builder.build();
    }

    private int count(List<GtfsRealtime.FeedEntity> vehicles, Predicate<GtfsRealtime.FeedEntity> predicate) {
        return (int) vehicles.stream().filter(predicate).count();
    }
}

package com.mytransitgps.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity;
import org.apache.ibatis.annotations.Insert;

/**
 * 中文名称：jb.vehicle_latest_state MyBatis-Plus Mapper。
 *
 * 功能说明：提供冻结表的基础查询与持久化入口；复杂批量和 UPSERT 由受控自定义 SQL 完成。
 */
public interface JbVehicleLatestStateMapper extends BaseMapper<JbVehicleLatestStateEntity> {

    @Insert("""
            INSERT INTO jb.vehicle_latest_state (
                uid, feed_uid, observation_uid, vehicle_id, vehicle_label, license_plate,
                trip_id, resolved_route_id, route_short_name, route_long_name, static_direction_id,
                latitude, longitude, geom, source_speed_raw, derived_speed_kmh,
                vehicle_timestamp_raw, vehicle_time, last_seen_time, freshness_seconds,
                analysis_eligible, spatial_eligible, qc_flags, create_time, update_time
            ) VALUES (
                #{uid}, #{feedUid}, #{observationUid}, #{vehicleId}, #{vehicleLabel}, #{licensePlate},
                #{tripId}, #{resolvedRouteId}, #{routeShortName}, #{routeLongName}, #{staticDirectionId},
                #{latitude}, #{longitude}, #{geom,typeHandler=com.mytransitgps.persistence.typehandler.PostgreSqlGeometryTypeHandler},
                #{sourceSpeedRaw}, #{derivedSpeedKmh}, #{vehicleTimestampRaw}, #{vehicleTime},
                #{lastSeenTime}, #{freshnessSeconds}, #{analysisEligible}, #{spatialEligible},
                #{qcFlags,typeHandler=com.mytransitgps.persistence.typehandler.PostgreSqlJsonbTypeHandler},
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (feed_uid, vehicle_id) DO UPDATE SET
                observation_uid = EXCLUDED.observation_uid,
                vehicle_label = EXCLUDED.vehicle_label,
                license_plate = EXCLUDED.license_plate,
                trip_id = EXCLUDED.trip_id,
                resolved_route_id = EXCLUDED.resolved_route_id,
                route_short_name = EXCLUDED.route_short_name,
                route_long_name = EXCLUDED.route_long_name,
                static_direction_id = EXCLUDED.static_direction_id,
                latitude = EXCLUDED.latitude,
                longitude = EXCLUDED.longitude,
                geom = EXCLUDED.geom,
                source_speed_raw = EXCLUDED.source_speed_raw,
                derived_speed_kmh = EXCLUDED.derived_speed_kmh,
                vehicle_timestamp_raw = EXCLUDED.vehicle_timestamp_raw,
                vehicle_time = EXCLUDED.vehicle_time,
                last_seen_time = EXCLUDED.last_seen_time,
                freshness_seconds = EXCLUDED.freshness_seconds,
                analysis_eligible = EXCLUDED.analysis_eligible,
                spatial_eligible = EXCLUDED.spatial_eligible,
                qc_flags = EXCLUDED.qc_flags,
                update_time = CURRENT_TIMESTAMP
            WHERE EXCLUDED.last_seen_time >= jb.vehicle_latest_state.last_seen_time
            """)
    int upsert(JbVehicleLatestStateEntity state);
}

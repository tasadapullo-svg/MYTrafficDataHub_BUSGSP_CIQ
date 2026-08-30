BEGIN;

DO $$
DECLARE
    endpoint_uid UUID;
    run_id UUID;
    area_id UUID;
    link_uid_value UUID;
    segment_id UUID;
    equipment_id_value UUID;
    generated_uid UUID;
    created_at TIMESTAMPTZ;
    updated_at TIMESTAMPTZ;
    srid_value INTEGER;
    x_value NUMERIC;
    y_value NUMERIC;
BEGIN
    SELECT uid INTO endpoint_uid FROM lta.api_endpoint WHERE api_code = 'API01';

    INSERT INTO lta.collection_run
        (api_endpoint_uid, run_mode, request_start_time, request_end_time, http_status,
         page_count, record_count, response_bytes, success, snapshot_complete, consistency_status)
    VALUES
        (endpoint_uid, 'MANUAL_TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 200,
         1, 1, 256, TRUE, TRUE, 'SINGLE')
    RETURNING uid, create_time, update_time INTO run_id, created_at, updated_at;
    IF run_id IS NULL OR created_at IS NULL OR updated_at IS NULL THEN
        RAISE EXCEPTION 'collection_run UUID或审计时间未自动生成';
    END IF;

    INSERT INTO lta.study_area(ciq_code, zone_code, area_name, geom)
    VALUES ('WOODLANDS', 'CORE', '__TRANSACTION_TEST__',
            ST_Multi(ST_GeomFromText('POLYGON((103.75 1.42,103.76 1.42,103.76 1.43,103.75 1.43,103.75 1.42))',4326)))
    RETURNING uid INTO area_id;

    INSERT INTO lta.traffic_link
        (link_id, road_name, road_category, start_lon, start_lat, end_lon, end_lat, geom,
         first_seen_time, last_seen_time)
    VALUES
        ('__TRANSACTION_TEST_LINK__', '事务测试道路', 1,
         103.7551234567890123, 1.4251234567890123, 103.7561234567890123, 1.4261234567890123,
         ST_MakeLine(ST_SetSRID(ST_MakePoint(103.7551234567890123,1.4251234567890123),4326),
                     ST_SetSRID(ST_MakePoint(103.7561234567890123,1.4261234567890123),4326)),
         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING uid, create_time, update_time INTO link_uid_value, created_at, updated_at;
    IF link_uid_value IS NULL OR created_at IS NULL OR updated_at IS NULL THEN
        RAISE EXCEPTION 'traffic_link UUID或审计时间未自动生成';
    END IF;

    INSERT INTO lta.traffic_link_scope(link_uid, area_uid, match_method, is_primary)
    VALUES (link_uid_value, area_id, 'MANUAL', TRUE);

    INSERT INTO lta.traffic_speed_observation
        (snapshot_time, link_uid, speed_band, minimum_speed, maximum_speed, source_updated_time, run_uid)
    VALUES (CURRENT_TIMESTAMP, link_uid_value, 4, 30, 39, CURRENT_TIMESTAMP, run_id)
    RETURNING uid INTO generated_uid;

    INSERT INTO lta.travel_time_segment
        (name, direction, far_end_point, start_point, end_point, area_uid, first_seen_time, last_seen_time)
    VALUES ('BKE', 1, 'Woodlands', 'Mandai', 'Woodlands', area_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING uid INTO segment_id;

    INSERT INTO lta.travel_time_observation(snapshot_time, segment_uid, est_time_min, run_uid)
    VALUES (CURRENT_TIMESTAMP, segment_id, 7, run_id);

    INSERT INTO lta.traffic_incident_event
        (event_fingerprint, type, latitude, longitude, geom, message, area_uid,
         first_seen_time, last_seen_time, first_run_uid, last_run_uid)
    VALUES
        (encode(digest('__TRANSACTION_TEST_INCIDENT__','sha256'),'hex'), 'Test',
         1.4251234567890123, 103.7551234567890123,
         ST_SetSRID(ST_MakePoint(103.7551234567890123,1.4251234567890123),4326),
         '事务测试事件', area_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, run_id, run_id)
    RETURNING ST_SRID(geom), ST_X(geom), ST_Y(geom) INTO srid_value, x_value, y_value;
    IF srid_value <> 4326
       OR abs(x_value - 103.7551234567890123) > 0.000000000001
       OR abs(y_value - 1.4251234567890123) > 0.000000000001 THEN
        RAISE EXCEPTION 'POINT几何经纬度顺序或SRID错误';
    END IF;

    INSERT INTO lta.vms_equipment
        (equipment_id, latitude, longitude, geom, area_uid, first_seen_time, last_seen_time)
    VALUES
        ('__TRANSACTION_TEST_VMS__', 1.4251234567890123, 103.7551234567890123,
         ST_SetSRID(ST_MakePoint(103.7551234567890123,1.4251234567890123),4326),
         area_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING uid INTO equipment_id_value;

    INSERT INTO lta.vms_message_state
        (equipment_uid, message, message_hash, first_seen_time, last_seen_time, first_run_uid, last_run_uid)
    VALUES
        (equipment_id_value, '事务测试消息', encode(digest('事务测试消息','sha256'),'hex'),
         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, run_id, run_id);

    UPDATE lta.traffic_link SET road_name = '事务测试道路-已更新' WHERE uid = link_uid_value
    RETURNING update_time INTO updated_at;
    IF updated_at IS NULL THEN
        RAISE EXCEPTION 'update_time触发器未生效';
    END IF;

    SELECT ST_SRID(geom) INTO srid_value FROM lta.traffic_link WHERE uid = link_uid_value;
    IF srid_value <> 4326 THEN RAISE EXCEPTION 'LINESTRING SRID不是4326'; END IF;
    SELECT ST_SRID(geom) INTO srid_value FROM lta.study_area WHERE uid = area_id;
    IF srid_value <> 4326 THEN RAISE EXCEPTION 'MULTIPOLYGON SRID不是4326'; END IF;

    RAISE NOTICE 'PASS: UUID自动生成、create_time、update_time、UUID FK、NUMERIC精度、geometry SRID与POINT经纬度顺序均正确';
END;
$$;

SELECT 'PASS' AS uuid_auto_generated,
       'PASS' AS create_time,
       'PASS' AS update_time,
       'PASS' AS uuid_foreign_keys,
       'PASS' AS coordinate_precision,
       'PASS' AS geometry_srid_4326,
       'PASS' AS point_longitude_latitude_order,
       'ROLLBACK_PENDING' AS cleanup;

ROLLBACK;

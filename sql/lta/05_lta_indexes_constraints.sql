CREATE INDEX IF NOT EXISTS idx_collection_run_endpoint ON lta.collection_run(api_endpoint_uid);
CREATE INDEX IF NOT EXISTS idx_collection_run_scheduled_time ON lta.collection_run(scheduled_time DESC);
CREATE INDEX IF NOT EXISTS idx_collection_run_request_start ON lta.collection_run(request_start_time DESC);
CREATE INDEX IF NOT EXISTS idx_collection_run_success ON lta.collection_run(success);
CREATE INDEX IF NOT EXISTS idx_collection_run_create_time ON lta.collection_run(create_time DESC);

CREATE INDEX IF NOT EXISTS idx_collection_page_log_run ON lta.collection_page_log(run_uid);
CREATE INDEX IF NOT EXISTS idx_collection_artifact_run ON lta.collection_artifact(run_uid);
CREATE INDEX IF NOT EXISTS idx_study_area_geom ON lta.study_area USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_traffic_link_road_name ON lta.traffic_link(road_name);
CREATE INDEX IF NOT EXISTS idx_traffic_link_road_category ON lta.traffic_link(road_category);
CREATE INDEX IF NOT EXISTS idx_traffic_link_geom ON lta.traffic_link USING GIST(geom);
CREATE INDEX IF NOT EXISTS idx_traffic_link_scope_area ON lta.traffic_link_scope(area_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_link_scope_link ON lta.traffic_link_scope(link_uid);

CREATE INDEX IF NOT EXISTS idx_traffic_speed_link_time ON lta.traffic_speed_observation(link_uid, snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_traffic_speed_snapshot_time ON lta.traffic_speed_observation(snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_traffic_speed_run ON lta.traffic_speed_observation(run_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_speed_snapshot_brin ON lta.traffic_speed_observation USING BRIN(snapshot_time);

CREATE INDEX IF NOT EXISTS idx_travel_time_segment_area ON lta.travel_time_segment(area_uid);
CREATE INDEX IF NOT EXISTS idx_travel_time_segment_time ON lta.travel_time_observation(segment_uid, snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_travel_time_snapshot ON lta.travel_time_observation(snapshot_time DESC);
CREATE INDEX IF NOT EXISTS idx_travel_time_run ON lta.travel_time_observation(run_uid);

CREATE INDEX IF NOT EXISTS idx_traffic_incident_active ON lta.traffic_incident_event(active);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_type ON lta.traffic_incident_event(type);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_first_seen ON lta.traffic_incident_event(first_seen_time);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_area ON lta.traffic_incident_event(area_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_first_run ON lta.traffic_incident_event(first_run_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_last_run ON lta.traffic_incident_event(last_run_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_incident_geom ON lta.traffic_incident_event USING GIST(geom);

CREATE INDEX IF NOT EXISTS idx_vms_equipment_area ON lta.vms_equipment(area_uid);
CREATE INDEX IF NOT EXISTS idx_vms_equipment_geom ON lta.vms_equipment USING GIST(geom);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vms_message_one_active ON lta.vms_message_state(equipment_uid) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_vms_message_equipment ON lta.vms_message_state(equipment_uid);
CREATE INDEX IF NOT EXISTS idx_vms_message_first_run ON lta.vms_message_state(first_run_uid);
CREATE INDEX IF NOT EXISTS idx_vms_message_last_run ON lta.vms_message_state(last_run_uid);
CREATE INDEX IF NOT EXISTS idx_vms_message_last_seen ON lta.vms_message_state(last_seen_time DESC);

CREATE INDEX IF NOT EXISTS idx_faulty_light_active ON lta.faulty_traffic_light_event(active);
CREATE INDEX IF NOT EXISTS idx_faulty_light_area ON lta.faulty_traffic_light_event(area_uid);
CREATE INDEX IF NOT EXISTS idx_faulty_light_first_run ON lta.faulty_traffic_light_event(first_run_uid);
CREATE INDEX IF NOT EXISTS idx_faulty_light_last_run ON lta.faulty_traffic_light_event(last_run_uid);
CREATE INDEX IF NOT EXISTS idx_road_work_road_name ON lta.road_work_event(road_name);
CREATE INDEX IF NOT EXISTS idx_road_work_start_date ON lta.road_work_event(start_date);
CREATE INDEX IF NOT EXISTS idx_road_work_end_date ON lta.road_work_event(end_date);
CREATE INDEX IF NOT EXISTS idx_road_work_active ON lta.road_work_event(active);
CREATE INDEX IF NOT EXISTS idx_road_work_area ON lta.road_work_event(area_uid);
CREATE INDEX IF NOT EXISTS idx_road_work_first_run ON lta.road_work_event(first_run_uid);
CREATE INDEX IF NOT EXISTS idx_road_work_last_run ON lta.road_work_event(last_run_uid);
CREATE INDEX IF NOT EXISTS idx_traffic_flow_run ON lta.traffic_flow_file(run_uid);
CREATE INDEX IF NOT EXISTS idx_road_opening_road_name ON lta.road_opening_event(road_name);
CREATE INDEX IF NOT EXISTS idx_road_opening_start_date ON lta.road_opening_event(start_date);
CREATE INDEX IF NOT EXISTS idx_road_opening_end_date ON lta.road_opening_event(end_date);
CREATE INDEX IF NOT EXISTS idx_road_opening_active ON lta.road_opening_event(active);
CREATE INDEX IF NOT EXISTS idx_road_opening_area ON lta.road_opening_event(area_uid);
CREATE INDEX IF NOT EXISTS idx_road_opening_first_run ON lta.road_opening_event(first_run_uid);
CREATE INDEX IF NOT EXISTS idx_road_opening_last_run ON lta.road_opening_event(last_run_uid);

DO $$
DECLARE
    table_name TEXT;
    trigger_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'api_endpoint','collection_run','collection_page_log','collection_artifact','study_area',
        'traffic_link','traffic_link_scope','traffic_speed_observation','travel_time_segment',
        'travel_time_observation','traffic_incident_event','vms_equipment','vms_message_state',
        'faulty_traffic_light_event','road_work_event','traffic_flow_file','road_opening_event'
    ] LOOP
        trigger_name := 'trg_' || table_name || '_update_time';
        EXECUTE format('DROP TRIGGER IF EXISTS %I ON lta.%I', trigger_name, table_name);
        EXECUTE format(
            'CREATE TRIGGER %I BEFORE UPDATE ON lta.%I FOR EACH ROW EXECUTE FUNCTION lta.set_update_time()',
            trigger_name, table_name
        );
    END LOOP;
END;
$$;

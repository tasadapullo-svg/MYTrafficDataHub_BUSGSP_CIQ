-- API01 TrafficSpeedBands database chain verification.
-- This script is read-only.

SELECT
    r.uid AS run_uid,
    r.run_mode,
    r.request_start_time,
    r.request_end_time,
    r.http_status,
    r.page_count,
    r.record_count,
    r.response_bytes,
    r.success,
    r.snapshot_complete,
    r.retry_count,
    r.consistency_status,
    r.error_message
FROM lta.collection_run r
JOIN lta.api_endpoint e ON e.uid = r.api_endpoint_uid
WHERE e.api_code = 'API01'
ORDER BY r.request_start_time DESC
LIMIT 10;

SELECT run_uid, COUNT(*) AS page_logs, SUM(record_count) AS records, SUM(response_bytes) AS bytes
FROM lta.collection_page_log
WHERE run_uid IN (
    SELECT r.uid
    FROM lta.collection_run r
    JOIN lta.api_endpoint e ON e.uid = r.api_endpoint_uid
    WHERE e.api_code = 'API01'
)
GROUP BY run_uid
ORDER BY run_uid DESC;

SELECT run_uid, COUNT(*) AS artifacts, SUM(file_size_bytes) AS raw_bytes, COUNT(sha256) AS sha256_count
FROM lta.collection_artifact
WHERE artifact_type = 'RAW_JSON'
GROUP BY run_uid
ORDER BY run_uid DESC;

SELECT ciq_code, zone_code, COUNT(*) AS active_areas,
       BOOL_AND(ST_SRID(geom) = 4326) AS srid_4326,
       BOOL_AND(ST_IsValid(geom)) AS geom_valid
FROM lta.study_area
WHERE active = TRUE
GROUP BY ciq_code, zone_code
ORDER BY ciq_code, zone_code;

SELECT COUNT(*) AS traffic_links,
       BOOL_AND(ST_SRID(geom) = 4326) AS srid_4326,
       BOOL_AND(ST_IsValid(geom)) AS geom_valid
FROM lta.traffic_link
WHERE active = TRUE;

SELECT a.ciq_code, a.zone_code, COUNT(DISTINCT s.link_uid) AS scoped_links
FROM lta.traffic_link_scope s
JOIN lta.study_area a ON a.uid = s.area_uid
WHERE s.active = TRUE
GROUP BY a.ciq_code, a.zone_code
ORDER BY a.ciq_code, CASE a.zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 ELSE 3 END;

SELECT COUNT(*) AS observations,
       COUNT(DISTINCT link_uid) AS observed_links,
       MIN(snapshot_time) AS first_snapshot,
       MAX(snapshot_time) AS latest_snapshot,
       COUNT(*) FILTER (WHERE speed_band IS NULL) AS null_speed_band
FROM lta.traffic_speed_observation;

SELECT snapshot_time, link_uid, COUNT(*) AS duplicates
FROM lta.traffic_speed_observation
GROUP BY snapshot_time, link_uid
HAVING COUNT(*) > 1;

SELECT o.uid AS observation_uid
FROM lta.traffic_speed_observation o
LEFT JOIN lta.traffic_link l ON l.uid = o.link_uid
LEFT JOIN lta.collection_run r ON r.uid = o.run_uid
WHERE l.uid IS NULL OR r.uid IS NULL
LIMIT 20;


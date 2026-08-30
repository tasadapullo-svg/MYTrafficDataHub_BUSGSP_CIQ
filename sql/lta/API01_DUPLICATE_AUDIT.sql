-- API01 TrafficSpeedBands duplicate audit.
-- Business uniqueness: one observation per source snapshot and traffic link.

SELECT COUNT(*) AS total_rows
FROM lta.traffic_speed_observation;

SELECT uid, COUNT(*) AS cnt
FROM lta.traffic_speed_observation
GROUP BY uid
HAVING COUNT(*) > 1;

SELECT snapshot_time, link_uid, COUNT(*) AS cnt
FROM lta.traffic_speed_observation
GROUP BY snapshot_time, link_uid
HAVING COUNT(*) > 1
ORDER BY cnt DESC, snapshot_time DESC;

SELECT COUNT(*) AS duplicate_groups,
       COALESCE(SUM(cnt - 1), 0) AS duplicate_rows
FROM (
    SELECT snapshot_time, link_uid, COUNT(*) AS cnt
    FROM lta.traffic_speed_observation
    GROUP BY snapshot_time, link_uid
    HAVING COUNT(*) > 1
) d;

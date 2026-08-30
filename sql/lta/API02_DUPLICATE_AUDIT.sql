-- API02 EstimatedTravelTimes duplicate audit.
-- Business uniqueness: one observation per source snapshot and travel-time segment.

SELECT COUNT(*) AS total_rows
FROM lta.travel_time_observation;

SELECT uid, COUNT(*) AS cnt
FROM lta.travel_time_observation
GROUP BY uid
HAVING COUNT(*) > 1;

SELECT snapshot_time, segment_uid, COUNT(*) AS cnt
FROM lta.travel_time_observation
GROUP BY snapshot_time, segment_uid
HAVING COUNT(*) > 1
ORDER BY cnt DESC, snapshot_time DESC;

SELECT COUNT(*) AS duplicate_groups,
       COALESCE(SUM(cnt - 1), 0) AS duplicate_rows
FROM (
    SELECT snapshot_time, segment_uid, COUNT(*) AS cnt
    FROM lta.travel_time_observation
    GROUP BY snapshot_time, segment_uid
    HAVING COUNT(*) > 1
) d;

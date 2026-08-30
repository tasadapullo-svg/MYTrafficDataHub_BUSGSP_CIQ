-- API03 TrafficIncidents duplicate audit.
-- Business uniqueness: one lifecycle row per normalized incident fingerprint.

SELECT COUNT(*) AS total_rows
FROM lta.traffic_incident_event;

SELECT uid, COUNT(*) AS cnt
FROM lta.traffic_incident_event
GROUP BY uid
HAVING COUNT(*) > 1;

SELECT event_fingerprint, COUNT(*) AS cnt
FROM lta.traffic_incident_event
GROUP BY event_fingerprint
HAVING COUNT(*) > 1
ORDER BY cnt DESC;

SELECT COUNT(*) AS duplicate_groups,
       COALESCE(SUM(cnt - 1), 0) AS duplicate_rows
FROM (
    SELECT event_fingerprint, COUNT(*) AS cnt
    FROM lta.traffic_incident_event
    GROUP BY event_fingerprint
    HAVING COUNT(*) > 1
) d;

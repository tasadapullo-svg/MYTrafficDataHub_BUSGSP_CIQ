-- CIQ duplicate review for API01/API02/API03.
-- This file is audit-only by default. Do not execute cleanup statements unless a human has reviewed the rows.

\i sql/lta/API01_DUPLICATE_AUDIT.sql
\i sql/lta/API02_DUPLICATE_AUDIT.sql
\i sql/lta/API03_DUPLICATE_AUDIT.sql

-- Suggested manual cleanup pattern, intentionally not executable as-is:
-- 1. Export duplicate groups.
-- 2. Keep the earliest create_time row per business key.
-- 3. Confirm affected run_uid and raw artifacts.
-- 4. Only then adapt and execute a DELETE inside an explicit transaction.
--
-- BEGIN;
-- DELETE FROM lta.traffic_speed_observation
-- WHERE uid IN (
--     SELECT uid
--     FROM (
--         SELECT uid,
--                ROW_NUMBER() OVER (PARTITION BY snapshot_time, link_uid ORDER BY create_time, uid) AS rn
--         FROM lta.traffic_speed_observation
--     ) x
--     WHERE rn > 1
-- );
-- ROLLBACK;

-- Woodlands/Tuas 官方地址锚点的 1/3/5 km PostGIS geography buffer。
-- 来源证据：docs/ciq/STUDY_AREA_SOURCE_EVIDENCE.md
-- 安全策略：只处理六个正式组合；发现既有不同 geometry、不同名称或重复行时立即中止。

BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '2min';
SELECT pg_advisory_xact_lock(hashtextextended('lta.study_area.official-ciq-seed-v1', 0));

DO $seed$
DECLARE
    target RECORD;
    checkpoint GEOMETRY;
    expected_geom GEOMETRY;
    matching_rows INTEGER;
    active_rows INTEGER;
    existing_name TEXT;
    existing_geom GEOMETRY;
BEGIN
    IF to_regclass('lta.study_area') IS NULL THEN
        RAISE EXCEPTION 'lta.study_area does not exist';
    END IF;

    FOR target IN
        SELECT *
        FROM (VALUES
            ('WOODLANDS', 'CORE',     'WOODLANDS_CORE_1KM',     'WOODLANDS CHECKPOINT', 103.7685958692307::double precision, 1.445646787399312::double precision, 1000),
            ('WOODLANDS', 'APPROACH', 'WOODLANDS_APPROACH_3KM', 'WOODLANDS CHECKPOINT', 103.7685958692307::double precision, 1.445646787399312::double precision, 3000),
            ('WOODLANDS', 'CORRIDOR', 'WOODLANDS_CORRIDOR_5KM', 'WOODLANDS CHECKPOINT', 103.7685958692307::double precision, 1.445646787399312::double precision, 5000),
            ('TUAS',      'CORE',     'TUAS_CORE_1KM',          'TUAS CHECKPOINT',      103.6354480925524::double precision, 1.347293201169603::double precision, 1000),
            ('TUAS',      'APPROACH', 'TUAS_APPROACH_3KM',      'TUAS CHECKPOINT',      103.6354480925524::double precision, 1.347293201169603::double precision, 3000),
            ('TUAS',      'CORRIDOR', 'TUAS_CORRIDOR_5KM',      'TUAS CHECKPOINT',      103.6354480925524::double precision, 1.347293201169603::double precision, 5000)
        ) AS v(ciq_code, zone_code, area_name, corridor_name, longitude, latitude, radius_m)
    LOOP
        checkpoint := ST_SetSRID(ST_MakePoint(target.longitude, target.latitude), 4326);
        expected_geom := ST_Multi(ST_Buffer(checkpoint::geography, target.radius_m)::geometry);

        SELECT COUNT(*) INTO active_rows
        FROM lta.study_area
        WHERE ciq_code = target.ciq_code AND zone_code = target.zone_code AND active = TRUE;

        IF active_rows > 1 THEN
            RAISE EXCEPTION 'Duplicate active study_area combination: %/%', target.ciq_code, target.zone_code;
        END IF;

        IF active_rows = 1 THEN
            SELECT area_name, geom INTO existing_name, existing_geom
            FROM lta.study_area
            WHERE ciq_code = target.ciq_code AND zone_code = target.zone_code AND active = TRUE;

            IF existing_name <> target.area_name OR NOT ST_Equals(existing_geom, expected_geom) THEN
                RAISE EXCEPTION 'Existing active polygon requires manual comparison; refusing overwrite: %/% name=%',
                    target.ciq_code, target.zone_code, existing_name;
            END IF;
            CONTINUE;
        END IF;

        SELECT COUNT(*) INTO matching_rows
        FROM lta.study_area
        WHERE ciq_code = target.ciq_code AND zone_code = target.zone_code AND area_name = target.area_name;

        IF matching_rows = 0 THEN
            INSERT INTO lta.study_area(ciq_code, zone_code, area_name, corridor_name, geom, active)
            VALUES (target.ciq_code, target.zone_code, target.area_name, target.corridor_name,
                    expected_geom, TRUE);
        ELSIF matching_rows = 1 THEN
            SELECT geom INTO existing_geom
            FROM lta.study_area
            WHERE ciq_code = target.ciq_code AND zone_code = target.zone_code AND area_name = target.area_name;

            IF NOT ST_Equals(existing_geom, expected_geom) THEN
                RAISE EXCEPTION 'Existing inactive polygon requires manual comparison; refusing overwrite: %/%',
                    target.ciq_code, target.zone_code;
            END IF;

            UPDATE lta.study_area
            SET active = TRUE, corridor_name = target.corridor_name, update_time = CURRENT_TIMESTAMP
            WHERE ciq_code = target.ciq_code AND zone_code = target.zone_code AND area_name = target.area_name;
        ELSE
            RAISE EXCEPTION 'Duplicate business rows require manual cleanup: %/%/%',
                target.ciq_code, target.zone_code, target.area_name;
        END IF;
    END LOOP;
END
$seed$;

COMMIT;

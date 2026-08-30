-- Woodlands/Tuas 六区 PostGIS 只读 QA。

WITH required(ciq_code, zone_code, display_name) AS (
    VALUES
        ('WOODLANDS','CORE','WOODLANDS_CORE_1KM'),
        ('WOODLANDS','APPROACH','WOODLANDS_APPROACH_3KM'),
        ('WOODLANDS','CORRIDOR','WOODLANDS_CORRIDOR_5KM'),
        ('TUAS','CORE','TUAS_CORE_1KM'),
        ('TUAS','APPROACH','TUAS_APPROACH_3KM'),
        ('TUAS','CORRIDOR','TUAS_CORRIDOR_5KM')
), qa AS (
    SELECT r.ciq_code, r.zone_code, r.display_name,
           COUNT(a.uid) AS active_count,
           MIN(a.uid::text) AS database_uid,
           BOOL_AND(a.geom IS NOT NULL) AS geom_not_null,
           BOOL_AND(ST_IsValid(a.geom)) AS geom_valid,
           BOOL_AND(NOT ST_IsEmpty(a.geom)) AS geom_not_empty,
           BOOL_AND(ST_SRID(a.geom) = 4326) AS srid_4326,
           BOOL_AND(GeometryType(a.geom) = 'MULTIPOLYGON') AS multipolygon,
           ROUND((MAX(ST_Area(a.geom::geography)) / 1000000.0)::numeric, 6) AS area_sq_km
    FROM required r
    LEFT JOIN lta.study_area a
      ON a.ciq_code = r.ciq_code AND a.zone_code = r.zone_code AND a.active = TRUE
    GROUP BY r.ciq_code, r.zone_code, r.display_name
)
SELECT *,
       active_count = 1 AND COALESCE(geom_not_null, FALSE) AND COALESCE(geom_valid, FALSE)
           AND COALESCE(geom_not_empty, FALSE) AND COALESCE(srid_4326, FALSE)
           AND COALESCE(multipolygon, FALSE) AS row_qa_pass
FROM qa
ORDER BY ciq_code, CASE zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 ELSE 3 END;

-- 面积递增与嵌套。
SELECT core.ciq_code,
       ST_Area(core.geom::geography) < ST_Area(approach.geom::geography)
           AND ST_Area(approach.geom::geography) < ST_Area(corridor.geom::geography) AS area_order_pass,
       ST_Covers(approach.geom, core.geom) AS approach_covers_core,
       ST_Covers(corridor.geom, approach.geom) AS corridor_covers_approach
FROM lta.study_area core
JOIN lta.study_area approach ON approach.ciq_code = core.ciq_code
                            AND approach.zone_code = 'APPROACH' AND approach.active
JOIN lta.study_area corridor ON corridor.ciq_code = core.ciq_code
                            AND corridor.zone_code = 'CORRIDOR' AND corridor.active
WHERE core.zone_code = 'CORE' AND core.active
  AND core.ciq_code IN ('WOODLANDS','TUAS');

-- 官方锚点必须命中本 CIQ 三层区域，且不得命中另一 CIQ 的 CORE。
WITH checkpoints(ciq_code, checkpoint_geom) AS (
    VALUES
        ('WOODLANDS', ST_SetSRID(ST_MakePoint(103.7685958692307, 1.445646787399312), 4326)),
        ('TUAS', ST_SetSRID(ST_MakePoint(103.6354480925524, 1.347293201169603), 4326))
)
SELECT c.ciq_code,
       COUNT(DISTINCT own.uid) FILTER (WHERE ST_Covers(own.geom, c.checkpoint_geom)) AS own_areas_covering_anchor,
       COUNT(DISTINCT other_core.uid) FILTER (WHERE ST_Covers(other_core.geom, c.checkpoint_geom)) AS other_core_hits,
       COUNT(DISTINCT own.uid) FILTER (WHERE ST_Covers(own.geom, c.checkpoint_geom)) = 3
           AND COUNT(DISTINCT other_core.uid) FILTER (WHERE ST_Covers(other_core.geom, c.checkpoint_geom)) = 0
           AS anchor_qa_pass
FROM checkpoints c
LEFT JOIN lta.study_area own ON own.ciq_code = c.ciq_code AND own.active
LEFT JOIN lta.study_area other_core ON other_core.ciq_code <> c.ciq_code
                                      AND other_core.zone_code = 'CORE' AND other_core.active
GROUP BY c.ciq_code;

-- 当前道路候选数；全为 0 时状态为 LINK_SCOPE_QA_PENDING_API01。
WITH required(ciq_code, zone_code) AS (
    VALUES ('WOODLANDS','CORE'), ('WOODLANDS','APPROACH'), ('WOODLANDS','CORRIDOR'),
           ('TUAS','CORE'), ('TUAS','APPROACH'), ('TUAS','CORRIDOR')
)
SELECT r.ciq_code, r.zone_code, COUNT(DISTINCT l.uid) AS intersecting_links,
       CASE WHEN COUNT(DISTINCT l.uid) = 0 THEN 'LINK_SCOPE_QA_PENDING_API01' ELSE 'AVAILABLE' END AS link_scope_status
FROM required r
LEFT JOIN lta.study_area a ON a.ciq_code = r.ciq_code AND a.zone_code = r.zone_code AND a.active
LEFT JOIN lta.traffic_link l ON l.geom IS NOT NULL AND ST_Intersects(a.geom, l.geom)
GROUP BY r.ciq_code, r.zone_code
ORDER BY r.ciq_code, CASE r.zone_code WHEN 'CORE' THEN 1 WHEN 'APPROACH' THEN 2 ELSE 3 END;

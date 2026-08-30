-- lta.study_area 修改前只读备份查询。
-- 用法示例（不要把密码写进命令或文件）：
-- psql "$TRAFFIC_DB_URL" -U "$TRAFFIC_DB_USERNAME" -f 00_backup_study_area.sql \
--   > study_area_backup_YYYYMMDD_HHMMSS.csv
-- 输出包含列名及可重放的 EWKT；本脚本不修改数据库。

COPY (
    SELECT
        uid,
        ciq_code,
        zone_code,
        area_name,
        corridor_name,
        ST_AsEWKT(geom) AS geom_ewkt,
        active,
        create_time,
        update_time
    FROM lta.study_area
    ORDER BY ciq_code, zone_code, area_name, uid
) TO STDOUT WITH (FORMAT CSV, HEADER TRUE, ENCODING 'UTF8');

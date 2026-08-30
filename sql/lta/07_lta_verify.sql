-- 1. 数据库与Schema
SELECT current_database() AS database,
       inet_server_addr() AS host,
       inet_server_port() AS port,
       version() AS postgres_version,
       current_user AS database_user;

-- 2. 17张业务表及中文表注释
SELECT n.nspname AS schema_name,
       c.relname AS table_name,
       obj_description(c.oid, 'pg_class') AS table_comment
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'lta' AND c.relkind = 'r'
ORDER BY c.relname;

-- 3. UUID主键完整性（预期17/17）
WITH business_tables AS (
    SELECT c.oid, c.relname
    FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'lta' AND c.relkind = 'r'
), pk AS (
    SELECT con.conrelid, a.attname, format_type(a.atttypid, a.atttypmod) AS data_type
    FROM pg_constraint con
    JOIN LATERAL unnest(con.conkey) k(attnum) ON TRUE
    JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = k.attnum
    WHERE con.contype = 'p'
)
SELECT b.relname AS table_name,
       p.attname AS pk_column,
       p.data_type AS pk_type,
       (p.attname = 'uid' AND p.data_type = 'uuid') AS pass
FROM business_tables b LEFT JOIN pk p ON p.conrelid = b.oid
ORDER BY b.relname;

WITH checks AS (
    SELECT c.relname,
           bool_or(a.attname = 'uid' AND a.atttypid = 'uuid'::regtype AND con.contype = 'p') AS pass
    FROM pg_class c
    JOIN pg_namespace n ON n.oid = c.relnamespace
    LEFT JOIN pg_constraint con ON con.conrelid = c.oid AND con.contype = 'p'
    LEFT JOIN LATERAL unnest(con.conkey) k(attnum) ON TRUE
    LEFT JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = k.attnum
    WHERE n.nspname = 'lta' AND c.relkind = 'r'
    GROUP BY c.relname
)
SELECT count(*) FILTER (WHERE pass) AS uuid_pk_pass,
       count(*) AS business_table_count
FROM checks;

-- 4. create_time/update_time完整性（预期各17/17）
SELECT count(DISTINCT table_name) FILTER (WHERE column_name = 'create_time' AND data_type = 'timestamp with time zone') AS create_time_pass,
       count(DISTINCT table_name) FILTER (WHERE column_name = 'update_time' AND data_type = 'timestamp with time zone') AS update_time_pass,
       count(DISTINCT table_name) AS business_table_count
FROM information_schema.columns
WHERE table_schema = 'lta';

-- 5. 所有FK及两端类型（预期全部uuid -> uuid）
SELECT child.relname AS child_table,
       ca.attname AS fk_column,
       format_type(ca.atttypid, ca.atttypmod) AS child_type,
       parent.relname AS parent_table,
       pa.attname AS parent_column,
       format_type(pa.atttypid, pa.atttypmod) AS parent_type,
       con.confdeltype IN ('a','r') AS no_cascade_delete,
       (ca.atttypid = 'uuid'::regtype AND pa.atttypid = 'uuid'::regtype) AS uuid_to_uuid
FROM pg_constraint con
JOIN pg_class child ON child.oid = con.conrelid
JOIN pg_namespace ns ON ns.oid = child.relnamespace
JOIN pg_class parent ON parent.oid = con.confrelid
JOIN LATERAL unnest(con.conkey, con.confkey) keys(child_attnum, parent_attnum) ON TRUE
JOIN pg_attribute ca ON ca.attrelid = child.oid AND ca.attnum = keys.child_attnum
JOIN pg_attribute pa ON pa.attrelid = parent.oid AND pa.attnum = keys.parent_attnum
WHERE ns.nspname = 'lta' AND con.contype = 'f'
ORDER BY child.relname, ca.attname;

-- 6. 经纬度精度（预期precision=20、scale=16）
SELECT table_name, column_name, data_type, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE table_schema = 'lta'
  AND column_name IN ('latitude','longitude','start_lon','start_lat','end_lon','end_lat')
ORDER BY table_name, ordinal_position;

-- 7. PostGIS geometry类型与SRID（预期全部4326）
SELECT f_table_schema AS schema_name,
       f_table_name AS table_name,
       f_geometry_column AS geometry_column,
       type AS geometry_type,
       srid
FROM geometry_columns
WHERE f_table_schema = 'lta'
ORDER BY f_table_name;

-- 8. 中文注释完整性（预期0、0）
SELECT
    (SELECT count(*)
     FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'lta' AND c.relkind = 'r'
       AND obj_description(c.oid, 'pg_class') IS NULL) AS missing_table_comment,
    (SELECT count(*)
     FROM pg_attribute a
     JOIN pg_class c ON c.oid = a.attrelid
     JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'lta' AND c.relkind = 'r'
       AND a.attnum > 0 AND NOT a.attisdropped
       AND col_description(c.oid, a.attnum) IS NULL) AS missing_column_comment;

-- 9. 禁止identity及序列型主键（预期0、0）
SELECT count(*) FILTER (WHERE is_identity = 'YES') AS identity_column_count,
       count(*) FILTER (WHERE column_default LIKE 'nextval(%') AS sequence_default_count
FROM information_schema.columns
WHERE table_schema = 'lta';

-- 10. 每表update_time触发器（预期17）
SELECT count(*) AS update_time_trigger_count
FROM pg_trigger t
JOIN pg_class c ON c.oid = t.tgrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'lta' AND NOT t.tgisinternal
  AND t.tgname LIKE 'trg_%_update_time';

-- 11. 索引清单，包含BTREE/GIST/BRIN及VMS部分唯一索引
SELECT tablename AS table_name, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'lta'
ORDER BY tablename, indexname;

-- 12. 初始化接口（预期8条）
SELECT api_code, api_name, endpoint_url, schedule_type, interval_minutes, schedule_rule, enabled
FROM lta.api_endpoint
ORDER BY api_code;

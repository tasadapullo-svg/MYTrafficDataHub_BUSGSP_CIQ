-- MYTrafficDataHub empty production schema verification
-- Run after 01_gps_ciq_schema_only.sql while connected to database "mytrafficdatahub".

DO $verify$
DECLARE
    item record;
    row_count bigint;
    actual_count integer;
BEGIN
    IF current_database() <> 'mytrafficdatahub' THEN
        RAISE WARNING 'Current database is %, expected mytrafficdatahub', current_database();
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pgcrypto') THEN
        RAISE EXCEPTION 'Missing extension: pgcrypto';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'postgis') THEN
        RAISE EXCEPTION 'Missing extension: postgis';
    END IF;

    FOR item IN
        SELECT * FROM (VALUES
            ('core', 4),
            ('jb', 12),
            ('kuching', 12),
            ('kl', 12),
            ('melaka', 12),
            ('lta', 17)
        ) AS expected(schema_name, table_count)
    LOOP
        SELECT count(*) INTO actual_count
        FROM information_schema.tables
        WHERE table_schema = item.schema_name
          AND table_type = 'BASE TABLE';

        IF actual_count <> item.table_count THEN
            RAISE EXCEPTION 'Schema %: expected % tables, found %',
                item.schema_name, item.table_count, actual_count;
        END IF;
    END LOOP;

    FOR item IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE schemaname IN ('core','jb','kuching','kl','melaka','lta')
        ORDER BY schemaname, tablename
    LOOP
        EXECUTE format('SELECT count(*) FROM %I.%I', item.schemaname, item.tablename)
        INTO row_count;

        IF row_count <> 0 THEN
            RAISE EXCEPTION 'Table %.% is not empty: % rows',
                item.schemaname, item.tablename, row_count;
        END IF;
    END LOOP;

    RAISE NOTICE 'PASS: 69 GPS/CIQ tables created and all tables contain zero rows.';
END
$verify$;

SELECT table_schema, count(*) AS table_count
FROM information_schema.tables
WHERE table_schema IN ('core','jb','kuching','kl','melaka','lta')
  AND table_type = 'BASE TABLE'
GROUP BY table_schema
ORDER BY table_schema;

SELECT
    (SELECT count(*)
     FROM pg_constraint c
     JOIN pg_namespace n ON n.oid = c.connamespace
     WHERE n.nspname IN ('core','jb','kuching','kl','melaka','lta')) AS constraint_count,
    (SELECT count(*)
     FROM pg_indexes
     WHERE schemaname IN ('core','jb','kuching','kl','melaka','lta')) AS total_index_count,
    (SELECT count(*)
     FROM pg_trigger t
     JOIN pg_class c ON c.oid = t.tgrelid
     JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname IN ('core','jb','kuching','kl','melaka','lta')
       AND NOT t.tgisinternal) AS user_trigger_count;


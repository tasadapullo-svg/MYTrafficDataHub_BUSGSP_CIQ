\set ON_ERROR_STOP on

BEGIN ISOLATION LEVEL SERIALIZABLE;

DO $preflight$
DECLARE
    target record;
    table_count integer;
    difference_count integer;
BEGIN
    IF EXISTS (
        SELECT 1 FROM (VALUES ('core'),('jb'),('kuching'),('kl'),('melaka'),('public'),('jbsp')) v(schema_name)
        WHERE NOT EXISTS (SELECT 1 FROM pg_namespace n WHERE n.nspname=v.schema_name)
    ) THEN
        RAISE EXCEPTION 'MISSING_TARGET_SCHEMA';
    END IF;

    IF (SELECT count(*) FROM information_schema.tables WHERE table_schema='core' AND table_type='BASE TABLE') <> 4
       OR (SELECT count(*) FROM information_schema.tables WHERE table_schema='jb' AND table_type='BASE TABLE') <> 12 THEN
        RAISE EXCEPTION 'REFERENCE_SCHEMA_CONFLICT';
    END IF;

    IF (SELECT count(*) FROM core.study_city c JOIN core.gtfs_feed f ON f.city_uid=c.uid
        WHERE (c.schema_name='jb' AND f.feed_id='mybas-johor')
           OR (c.schema_name='kuching' AND f.feed_id='mybas-kuching')
           OR (c.schema_name='kl' AND f.feed_id IN ('rapid-bus-kl','rapid-bus-mrtfeeder'))
           OR (c.schema_name='melaka' AND f.feed_id='mybas-melaka')) <> 5 THEN
        RAISE EXCEPTION 'CORE_SEED_CONFLICT';
    END IF;

    FOR target IN SELECT * FROM (VALUES
        ('kuching','Kuching'),('kl','Kuala Lumpur'),('melaka','Melaka')
    ) v(schema_name, city_name) LOOP
        SELECT count(*) INTO table_count FROM information_schema.tables
        WHERE table_schema=target.schema_name AND table_type='BASE TABLE';
        IF table_count NOT IN (0,12) THEN
            RAISE EXCEPTION 'PARTIAL_SCHEMA_CONFLICT: schema=%, tables=%', target.schema_name, table_count;
        END IF;
        IF table_count=12 THEN
            SELECT count(*) INTO difference_count FROM (
                (SELECT table_name,ordinal_position,column_name,data_type,udt_name,is_nullable,column_default
                 FROM information_schema.columns WHERE table_schema='jb'
                 EXCEPT
                 SELECT table_name,ordinal_position,column_name,data_type,udt_name,is_nullable,column_default
                 FROM information_schema.columns WHERE table_schema=target.schema_name)
                UNION ALL
                (SELECT table_name,ordinal_position,column_name,data_type,udt_name,is_nullable,column_default
                 FROM information_schema.columns WHERE table_schema=target.schema_name
                 EXCEPT
                 SELECT table_name,ordinal_position,column_name,data_type,udt_name,is_nullable,column_default
                 FROM information_schema.columns WHERE table_schema='jb')
            ) d;
            IF difference_count<>0 THEN
                RAISE EXCEPTION 'PARTIAL_SCHEMA_CONFLICT: schema=%, column_difference=%', target.schema_name, difference_count;
            END IF;

            SELECT count(*) INTO difference_count FROM (
                SELECT c.relname,p.conname,p.contype,
                       replace(pg_get_constraintdef(p.oid,true),target.schema_name||'.','jb.') definition
                FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname=target.schema_name
                EXCEPT
                SELECT c.relname,p.conname,p.contype,pg_get_constraintdef(p.oid,true)
                FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
                WHERE n.nspname='jb'
            ) d;
            IF difference_count<>0 THEN
                RAISE EXCEPTION 'PARTIAL_SCHEMA_CONFLICT: schema=%, constraint_difference=%', target.schema_name, difference_count;
            END IF;

            SELECT count(*) INTO difference_count FROM (
                SELECT tablename,indexname,replace(indexdef,' ON '||target.schema_name||'.',' ON jb.') definition
                FROM pg_indexes WHERE schemaname=target.schema_name
                EXCEPT
                SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='jb'
            ) d;
            IF difference_count<>0 THEN
                RAISE EXCEPTION 'PARTIAL_SCHEMA_CONFLICT: schema=%, index_difference=%', target.schema_name, difference_count;
            END IF;

            SELECT count(*) INTO difference_count FROM (
                SELECT c.relname,
                       replace(obj_description(j.oid,'pg_class'),'Johor Bahru',target.city_name) expected,
                       obj_description(c.oid,'pg_class') actual
                FROM pg_class j JOIN pg_namespace jn ON jn.oid=j.relnamespace AND jn.nspname='jb'
                JOIN pg_class c ON c.relname=j.relname JOIN pg_namespace n ON n.oid=c.relnamespace AND n.nspname=target.schema_name
                WHERE j.relkind='r' AND obj_description(c.oid,'pg_class') IS DISTINCT FROM
                      replace(obj_description(j.oid,'pg_class'),'Johor Bahru',target.city_name)
            ) d;
            IF difference_count<>0 THEN
                RAISE EXCEPTION 'PARTIAL_SCHEMA_CONFLICT: schema=%, comment_difference=%', target.schema_name, difference_count;
            END IF;
        END IF;
    END LOOP;
END
$preflight$;

COMMIT;


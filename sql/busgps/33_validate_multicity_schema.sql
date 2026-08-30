\set ON_ERROR_STOP on

DO $validate$
DECLARE target_schema text; table_item record; n integer; total_rows bigint; sql_text text;
BEGIN
  IF (SELECT count(*) FROM information_schema.tables WHERE table_schema IN ('core','jb','kuching','kl','melaka') AND table_type='BASE TABLE')<>52 THEN
    RAISE EXCEPTION 'VALIDATION_FAILED: total physical tables must be 52';
  END IF;
  FOREACH target_schema IN ARRAY ARRAY['kuching','kl','melaka'] LOOP
    IF (SELECT count(*) FROM information_schema.tables WHERE table_schema=target_schema AND table_type='BASE TABLE')<>12 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.table_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM information_schema.columns WHERE table_schema=target_schema)<>302 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.column_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM pg_indexes WHERE schemaname=target_schema)<>(SELECT count(*) FROM pg_indexes WHERE schemaname='jb') THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.index_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM pg_constraint WHERE connamespace=target_schema::regnamespace AND contype='f')<>33 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.foreign_key_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM pg_constraint WHERE connamespace=target_schema::regnamespace AND contype='p')<>12 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.primary_key_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM geometry_columns WHERE f_table_schema=target_schema)<>5 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.geometry_count',target_schema;
    END IF;
    IF (SELECT count(*) FROM pg_class c JOIN pg_namespace ns ON ns.oid=c.relnamespace
        WHERE ns.nspname=target_schema AND c.relkind='r' AND obj_description(c.oid,'pg_class') IS NOT NULL)<>12 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.table_comments',target_schema;
    END IF;
    IF (SELECT count(*) FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace ns ON ns.oid=c.relnamespace
        WHERE ns.nspname=target_schema AND c.relkind='r' AND a.attnum>0 AND NOT a.attisdropped AND col_description(c.oid,a.attnum) IS NOT NULL)<>302 THEN
      RAISE EXCEPTION 'VALIDATION_FAILED: %.column_comments',target_schema;
    END IF;
    total_rows:=0;
    FOR table_item IN SELECT table_name FROM information_schema.tables WHERE table_schema=target_schema AND table_type='BASE TABLE' LOOP
      EXECUTE format('SELECT count(*) FROM %I.%I',target_schema,table_item.table_name) INTO n;
      total_rows:=total_rows+n;
    END LOOP;
    IF total_rows<>0 THEN RAISE EXCEPTION 'VALIDATION_FAILED: %.business_rows=%',target_schema,total_rows; END IF;
  END LOOP;
  IF EXISTS (
    SELECT 1 FROM pg_constraint f JOIN pg_class s ON s.oid=f.conrelid JOIN pg_namespace sn ON sn.oid=s.relnamespace
    JOIN pg_class t ON t.oid=f.confrelid JOIN pg_namespace tn ON tn.oid=t.relnamespace
    WHERE f.contype='f' AND sn.nspname IN ('kuching','kl','melaka')
      AND tn.nspname IN ('jb','kuching','kl','melaka') AND tn.nspname<>sn.nspname
  ) THEN RAISE EXCEPTION 'VALIDATION_FAILED: INVALID_CROSS_CITY_FK'; END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema IN ('core','jb','kuching','kl','melaka') AND data_type='timestamp without time zone') THEN
    RAISE EXCEPTION 'VALIDATION_FAILED: timestamp without time zone found';
  END IF;
END
$validate$;

SELECT table_schema,count(*) table_count FROM information_schema.tables
WHERE table_schema IN ('core','jb','kuching','kl','melaka') AND table_type='BASE TABLE'
GROUP BY table_schema ORDER BY table_schema;


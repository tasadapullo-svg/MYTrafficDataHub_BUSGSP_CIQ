\set ON_ERROR_STOP on
BEGIN;
DO $indexes$
DECLARE target_schema text; item record; ddl text;
BEGIN
  FOREACH target_schema IN ARRAY ARRAY['kuching','kl','melaka'] LOOP
    FOR item IN
      SELECT t.relname table_name,i.relname index_name,pg_get_indexdef(i.oid) definition
      FROM pg_index x JOIN pg_class i ON i.oid=x.indexrelid JOIN pg_class t ON t.oid=x.indrelid
      JOIN pg_namespace n ON n.oid=t.relnamespace LEFT JOIN pg_constraint p ON p.conindid=i.oid
      WHERE n.nspname='jb' AND p.oid IS NULL ORDER BY t.relname,i.relname
    LOOP
      IF NOT EXISTS (SELECT 1 FROM pg_class i JOIN pg_namespace n ON n.oid=i.relnamespace
                     WHERE n.nspname=target_schema AND i.relkind='i' AND i.relname=item.index_name) THEN
        ddl := replace(item.definition,' ON jb.',' ON '||quote_ident(target_schema)||'.');
        ddl := regexp_replace(ddl,'^CREATE UNIQUE INDEX ','CREATE UNIQUE INDEX IF NOT EXISTS ');
        ddl := regexp_replace(ddl,'^CREATE INDEX ','CREATE INDEX IF NOT EXISTS ');
        EXECUTE ddl;
      END IF;
    END LOOP;
  END LOOP;
END
$indexes$;
COMMIT;


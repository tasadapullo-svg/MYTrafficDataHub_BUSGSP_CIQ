\set ON_ERROR_STOP on
BEGIN;
DO $comments$
DECLARE target record; item record; translated text;
BEGIN
  FOR target IN SELECT * FROM (VALUES ('kuching','Kuching'),('kl','Kuala Lumpur'),('melaka','Melaka')) v(schema_name,city_name) LOOP
    FOR item IN
      SELECT c.relname table_name,obj_description(c.oid,'pg_class') comment_text
      FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='jb' AND c.relkind='r' ORDER BY c.relname
    LOOP
      translated := replace(item.comment_text,'Johor Bahru',target.city_name);
      EXECUTE format('COMMENT ON TABLE %I.%I IS %L',target.schema_name,item.table_name,translated);
    END LOOP;
    FOR item IN
      SELECT c.relname table_name,a.attname column_name,col_description(c.oid,a.attnum) comment_text
      FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='jb' AND c.relkind='r' AND a.attnum>0 AND NOT a.attisdropped ORDER BY c.relname,a.attnum
    LOOP
      translated := replace(replace(item.comment_text,'Johor Bahru',target.city_name),'jb.',target.schema_name||'.');
      EXECUTE format('COMMENT ON COLUMN %I.%I.%I IS %L',target.schema_name,item.table_name,item.column_name,translated);
    END LOOP;
  END LOOP;
END
$comments$;
COMMIT;


\set ON_ERROR_STOP on
BEGIN;
DO $foreign_keys$
DECLARE target_schema text; item record; definition text;
BEGIN
  FOREACH target_schema IN ARRAY ARRAY['kuching','kl','melaka'] LOOP
    FOR item IN
      SELECT c.relname table_name,p.conname,pg_get_constraintdef(p.oid,true) definition
      FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='jb' AND p.contype='f' ORDER BY c.relname,p.conname
    LOOP
      IF NOT EXISTS (SELECT 1 FROM pg_constraint p WHERE p.connamespace=target_schema::regnamespace AND p.conname=item.conname) THEN
        definition := replace(item.definition,'REFERENCES jb.','REFERENCES '||quote_ident(target_schema)||'.');
        EXECUTE format('ALTER TABLE %I.%I ADD CONSTRAINT %I %s',target_schema,item.table_name,item.conname,definition);
      END IF;
    END LOOP;
  END LOOP;
END
$foreign_keys$;
COMMIT;


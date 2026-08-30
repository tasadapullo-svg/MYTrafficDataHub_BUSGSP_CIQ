\set ON_ERROR_STOP on
SELECT table_schema,count(*) table_count
FROM information_schema.tables WHERE table_schema IN ('core','jb','kuching','kl','melaka') AND table_type='BASE TABLE'
GROUP BY table_schema ORDER BY table_schema;
SELECT schemaname,count(*) index_count FROM pg_indexes
WHERE schemaname IN ('core','jb','kuching','kl','melaka') GROUP BY schemaname ORDER BY schemaname;
SELECT n.nspname schema_name,c.contype,count(*) constraint_count
FROM pg_constraint c JOIN pg_namespace n ON n.oid=c.connamespace
WHERE n.nspname IN ('core','jb','kuching','kl','melaka') GROUP BY n.nspname,c.contype ORDER BY n.nspname,c.contype;
SELECT n.nspname schema_name,c.relkind,count(*) object_count
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname IN ('kuching','kl','melaka') AND c.relkind IN ('v','m','S')
GROUP BY n.nspname,c.relkind ORDER BY n.nspname,c.relkind;


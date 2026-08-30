\set ON_ERROR_STOP on

-- 本文件必须在 PostgreSQL 容器内执行；所有导出直接读取真实 information_schema/pg_catalog。
COPY (
WITH base AS (
  SELECT ic.table_schema schema_name,ic.table_name,obj_description(c.oid,'pg_class') table_comment,
         ic.ordinal_position,ic.column_name,ic.data_type,ic.udt_name,format_type(a.atttypid,a.atttypmod) formatted_type,
         ic.character_maximum_length,ic.numeric_precision,ic.numeric_scale,ic.is_nullable,ic.column_default,
         c.oid table_oid,a.attnum,col_description(c.oid,a.attnum) column_comment
  FROM information_schema.columns ic JOIN pg_namespace n ON n.nspname=ic.table_schema
  JOIN pg_class c ON c.relnamespace=n.oid AND c.relname=ic.table_name AND c.relkind='r'
  JOIN pg_attribute a ON a.attrelid=c.oid AND a.attname=ic.column_name
  WHERE ic.table_schema IN ('core','jb','kuching','kl','melaka')
)
SELECT b.schema_name,b.table_name,b.table_comment,b.ordinal_position,b.column_name,b.data_type,b.udt_name,b.formatted_type,
       b.character_maximum_length,b.numeric_precision,b.numeric_scale,b.is_nullable,b.column_default,
       EXISTS (SELECT 1 FROM pg_constraint p WHERE p.conrelid=b.table_oid AND p.contype='p' AND b.attnum=ANY(p.conkey)) is_primary_key,
       fk.constraint_name IS NOT NULL is_foreign_key,fk.foreign_schema,fk.foreign_table,fk.foreign_column,
       coalesce(uq.is_single_column_unique,false) is_single_column_unique,uq.unique_constraint_names,
       uq.unique_constraint_columns,uq.unique_predicate,b.column_comment
FROM base b
LEFT JOIN LATERAL (
  SELECT p.conname constraint_name,tn.nspname foreign_schema,tt.relname foreign_table,ta.attname foreign_column
  FROM pg_constraint p JOIN pg_class tt ON tt.oid=p.confrelid JOIN pg_namespace tn ON tn.oid=tt.relnamespace
  JOIN LATERAL unnest(p.conkey,p.confkey) k(source_attnum,target_attnum) ON true
  JOIN pg_attribute ta ON ta.attrelid=tt.oid AND ta.attnum=k.target_attnum
  WHERE p.conrelid=b.table_oid AND p.contype='f' AND k.source_attnum=b.attnum LIMIT 1
) fk ON true
LEFT JOIN LATERAL (
  SELECT bool_or(cardinality(q.keys)=1) is_single_column_unique,
         string_agg(q.name,';' ORDER BY q.name) unique_constraint_names,
         string_agg(q.columns,';' ORDER BY q.name) unique_constraint_columns,
         string_agg(coalesce(q.predicate,''),';' ORDER BY q.name) unique_predicate
  FROM (
    SELECT p.conname name,p.conkey keys,
           array_to_string(ARRAY(SELECT aa.attname FROM unnest(p.conkey) WITH ORDINALITY z(attnum,ord)
                                 JOIN pg_attribute aa ON aa.attrelid=p.conrelid AND aa.attnum=z.attnum ORDER BY z.ord),',') columns,
           NULL::text predicate
    FROM pg_constraint p WHERE p.conrelid=b.table_oid AND p.contype='u' AND b.attnum=ANY(p.conkey)
    UNION ALL
    SELECT ix.relname,i.indkey::smallint[],
           array_to_string(ARRAY(SELECT pg_get_indexdef(i.indexrelid,z.ord,true)
                                 FROM generate_series(1,i.indnkeyatts) z(ord)),','),
           pg_get_expr(i.indpred,i.indrelid)
    FROM pg_index i JOIN pg_class ix ON ix.oid=i.indexrelid LEFT JOIN pg_constraint p ON p.conindid=i.indexrelid
    WHERE i.indrelid=b.table_oid AND i.indisunique AND p.oid IS NULL AND b.attnum=ANY(i.indkey::smallint[])
  ) q
) uq ON true
ORDER BY b.schema_name,b.table_name,b.ordinal_position
) TO '/tmp/MYTransitGPS_multicity_table_columns.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');

COPY (
SELECT n.nspname schema_name,t.relname table_name,i.relname index_name,am.amname index_method,
       array_to_string(ARRAY(SELECT pg_get_indexdef(x.indexrelid,g.n,true) FROM generate_series(1,x.indnkeyatts) g(n)),',') index_columns,
       x.indisunique is_unique,pg_get_expr(x.indpred,x.indrelid) predicate,pg_get_indexdef(i.oid) index_definition
FROM pg_index x JOIN pg_class i ON i.oid=x.indexrelid JOIN pg_class t ON t.oid=x.indrelid
JOIN pg_namespace n ON n.oid=t.relnamespace JOIN pg_am am ON am.oid=i.relam
WHERE n.nspname IN ('core','jb','kuching','kl','melaka') ORDER BY n.nspname,t.relname,i.relname
) TO '/tmp/MYTransitGPS_multicity_indexes.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');

COPY (
SELECT n.nspname schema_name,t.relname table_name,c.conname constraint_name,
       CASE c.contype WHEN 'p' THEN 'PRIMARY KEY' WHEN 'f' THEN 'FOREIGN KEY' WHEN 'u' THEN 'UNIQUE' WHEN 'c' THEN 'CHECK' WHEN 'x' THEN 'EXCLUDE' END constraint_type,
       array_to_string(ARRAY(SELECT a.attname FROM unnest(c.conkey) WITH ORDINALITY k(attnum,ord)
                             JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=k.attnum ORDER BY k.ord),',') columns,
       rn.nspname referenced_schema,rt.relname referenced_table,
       array_to_string(ARRAY(SELECT a.attname FROM unnest(c.confkey) WITH ORDINALITY k(attnum,ord)
                             JOIN pg_attribute a ON a.attrelid=c.confrelid AND a.attnum=k.attnum ORDER BY k.ord),',') referenced_columns,
       CASE c.confupdtype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT' WHEN 'c' THEN 'CASCADE' WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' END update_rule,
       CASE c.confdeltype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT' WHEN 'c' THEN 'CASCADE' WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' END delete_rule,
       pg_get_constraintdef(c.oid,true) definition
FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
LEFT JOIN pg_class rt ON rt.oid=c.confrelid LEFT JOIN pg_namespace rn ON rn.oid=rt.relnamespace
WHERE n.nspname IN ('core','jb','kuching','kl','melaka') ORDER BY n.nspname,t.relname,c.conname
) TO '/tmp/MYTransitGPS_multicity_constraints.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');

COPY (
WITH fks AS (
 SELECT c.*,n.nspname source_schema,t.relname source_table,rn.nspname target_schema,rt.relname target_table
 FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace
 JOIN pg_class rt ON rt.oid=c.confrelid JOIN pg_namespace rn ON rn.oid=rt.relnamespace
 WHERE c.contype='f' AND n.nspname IN ('core','jb','kuching','kl','melaka')
)
SELECT f.source_schema,f.source_table,f.conname constraint_name,
       array_to_string(ARRAY(SELECT a.attname FROM unnest(f.conkey) WITH ORDINALITY k(attnum,ord)
                             JOIN pg_attribute a ON a.attrelid=f.conrelid AND a.attnum=k.attnum ORDER BY k.ord),',') source_columns,
       f.target_schema,f.target_table,
       array_to_string(ARRAY(SELECT a.attname FROM unnest(f.confkey) WITH ORDINALITY k(attnum,ord)
                             JOIN pg_attribute a ON a.attrelid=f.confrelid AND a.attnum=k.attnum ORDER BY k.ord),',') target_columns,
       CASE f.confupdtype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT' WHEN 'c' THEN 'CASCADE' WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' END on_update,
       CASE f.confdeltype WHEN 'a' THEN 'NO ACTION' WHEN 'r' THEN 'RESTRICT' WHEN 'c' THEN 'CASCADE' WHEN 'n' THEN 'SET NULL' WHEN 'd' THEN 'SET DEFAULT' END on_delete,
       sx.index_name supporting_index,sx.index_method,f.convalidated valid
FROM fks f LEFT JOIN LATERAL (
 SELECT ix.relname index_name,am.amname index_method FROM pg_index i JOIN pg_class ix ON ix.oid=i.indexrelid JOIN pg_am am ON am.oid=ix.relam
 WHERE i.indrelid=f.conrelid AND i.indisvalid AND i.indisready
   AND (i.indkey::smallint[])[0:cardinality(f.conkey)-1]=f.conkey ORDER BY i.indisunique DESC,ix.relname LIMIT 1
) sx ON true ORDER BY f.source_schema,f.source_table,f.conname
) TO '/tmp/MYTransitGPS_multicity_foreign_keys.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');

COPY (
SELECT n.nspname schema_name,'TABLE' object_type,c.relname table_name,NULL::text column_name,obj_description(c.oid,'pg_class') comment
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname IN ('core','jb','kuching','kl','melaka') AND c.relkind='r'
UNION ALL
SELECT n.nspname,'COLUMN',c.relname,a.attname,col_description(c.oid,a.attnum)
FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname IN ('core','jb','kuching','kl','melaka') AND c.relkind='r' AND a.attnum>0 AND NOT a.attisdropped
ORDER BY 1,3,2 DESC,4 NULLS FIRST
) TO '/tmp/MYTransitGPS_multicity_comments.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');

COPY (
WITH targets(target_schema) AS (VALUES ('kuching'),('kl'),('melaka')),
tables AS (SELECT table_name FROM information_schema.tables WHERE table_schema='jb' AND table_type='BASE TABLE'),
checks AS (
SELECT 'jb' reference_schema,tg.target_schema,t.table_name,
 (SELECT count(*) FROM information_schema.columns WHERE table_schema='jb' AND table_name=t.table_name)=
 (SELECT count(*) FROM information_schema.columns WHERE table_schema=tg.target_schema AND table_name=t.table_name) column_count_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT ordinal_position,column_name,udt_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema='jb' AND table_name=t.table_name) EXCEPT (SELECT ordinal_position,column_name,udt_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema=tg.target_schema AND table_name=t.table_name)) d) column_definition_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT conname,pg_get_constraintdef(p.oid,true) FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='jb' AND c.relname=t.table_name AND p.contype='p') EXCEPT (SELECT conname,replace(pg_get_constraintdef(p.oid,true),tg.target_schema||'.','jb.') FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name AND p.contype='p')) d) pk_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT conname,pg_get_constraintdef(p.oid,true) FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='jb' AND c.relname=t.table_name AND p.contype='u') EXCEPT (SELECT conname,replace(pg_get_constraintdef(p.oid,true),tg.target_schema||'.','jb.') FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name AND p.contype='u')) d) unique_match,
 (SELECT count(*) FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='jb' AND c.relname=t.table_name AND p.contype='f')=
 (SELECT count(*) FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name AND p.contype='f') fk_count_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT conname,pg_get_constraintdef(p.oid,true) FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='jb' AND c.relname=t.table_name AND p.contype='f') EXCEPT (SELECT conname,replace(pg_get_constraintdef(p.oid,true),tg.target_schema||'.','jb.') FROM pg_constraint p JOIN pg_class c ON c.oid=p.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name AND p.contype='f')) d) fk_semantic_match,
 (SELECT count(*) FROM pg_indexes WHERE schemaname='jb' AND tablename=t.table_name)=
 (SELECT count(*) FROM pg_indexes WHERE schemaname=tg.target_schema AND tablename=t.table_name) index_count_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='jb' AND tablename=t.table_name) EXCEPT (SELECT indexname,replace(indexdef,' ON '||tg.target_schema||'.',' ON jb.') FROM pg_indexes WHERE schemaname=tg.target_schema AND tablename=t.table_name)) d) index_definition_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT f_geometry_column,type,srid FROM geometry_columns WHERE f_table_schema='jb' AND f_table_name=t.table_name) EXCEPT (SELECT f_geometry_column,type,srid FROM geometry_columns WHERE f_table_schema=tg.target_schema AND f_table_name=t.table_name)) d) geometry_match,
 NOT EXISTS (SELECT 1 FROM ((SELECT column_name FROM information_schema.columns WHERE table_schema='jb' AND table_name=t.table_name AND udt_name='jsonb') EXCEPT (SELECT column_name FROM information_schema.columns WHERE table_schema=tg.target_schema AND table_name=t.table_name AND udt_name='jsonb')) d) jsonb_match,
 (SELECT obj_description(c.oid,'pg_class') IS NOT NULL FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name) AND
 NOT EXISTS (SELECT 1 FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname=tg.target_schema AND c.relname=t.table_name AND a.attnum>0 AND NOT a.attisdropped AND col_description(c.oid,a.attnum) IS NULL) comment_complete
FROM targets tg CROSS JOIN tables t)
SELECT *,CASE WHEN column_count_match AND column_definition_match AND pk_match AND unique_match AND fk_count_match AND fk_semantic_match AND index_count_match AND index_definition_match AND geometry_match AND jsonb_match AND comment_complete THEN 'PASS' ELSE 'FAIL' END overall_status
FROM checks ORDER BY target_schema,table_name
) TO '/tmp/MYTransitGPS_city_schema_parity.csv' WITH (FORMAT csv,HEADER true,ENCODING 'UTF8');


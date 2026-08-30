package com.mytransitgps.persistence.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：数据库结构指纹服务。
 *
 * 功能：只读提取 52 张项目表的列、约束、索引和注释并计算稳定 SHA-256；
 * 输入为冻结数据库元数据，输出为前后对照指纹；绝不执行 DDL。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class SchemaFingerprintService {

    private static final Logger log = LoggerFactory.getLogger(SchemaFingerprintService.class);
    private final JdbcTemplate jdbc;

    public SchemaFingerprintService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> capture() {
        // 对表、列、约束和索引的规范化元数据计算指纹，用于发现采集期间的意外 DDL。
        log.info("开始采集数据库Schema结构指纹，schemas=core,jb,kuching,kl,melaka");
        String sql = """
                WITH objects AS (
                  SELECT 'column' kind,c.table_schema schema_name,c.table_name object_name,
                    concat_ws('|',c.ordinal_position,c.column_name,c.data_type,c.udt_name,c.is_nullable,c.column_default) detail
                  FROM information_schema.columns c
                  WHERE c.table_schema IN ('core','jb','kuching','kl','melaka')
                  UNION ALL
                  SELECT 'constraint',n.nspname,r.relname,concat_ws('|',con.conname,con.contype,pg_get_constraintdef(con.oid,true))
                  FROM pg_constraint con JOIN pg_class r ON r.oid=con.conrelid JOIN pg_namespace n ON n.oid=r.relnamespace
                  WHERE n.nspname IN ('core','jb','kuching','kl','melaka')
                  UNION ALL
                  SELECT 'index',schemaname,tablename,concat_ws('|',indexname,indexdef)
                  FROM pg_indexes WHERE schemaname IN ('core','jb','kuching','kl','melaka')
                  UNION ALL
                  SELECT 'table_comment',n.nspname,c.relname,coalesce(obj_description(c.oid,'pg_class'),'')
                  FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                  WHERE c.relkind='r' AND n.nspname IN ('core','jb','kuching','kl','melaka')
                  UNION ALL
                  SELECT 'column_comment',n.nspname,c.relname,concat_ws('|',a.attname,coalesce(col_description(c.oid,a.attnum),''))
                  FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                  JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
                  WHERE c.relkind='r' AND n.nspname IN ('core','jb','kuching','kl','melaka')
                ) SELECT kind,schema_name,object_name,detail FROM objects ORDER BY kind,schema_name,object_name,detail
                """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        StringBuilder canonical = new StringBuilder();
        rows.forEach(row -> canonical.append(row.get("kind")).append('|').append(row.get("schema_name"))
                .append('|').append(row.get("object_name")).append('|').append(row.get("detail")).append('\n'));
        try {
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
            log.info("数据库Schema结构指纹采集完成，sha256={}，metadataRows={}，expectedTableCount=52", sha, rows.size());
            return Map.of("sha256", sha, "metadata_row_count", rows.size(), "table_count", 52);
        } catch (Exception ex) {
            log.error("数据库Schema结构指纹计算失败，错误信息={}", ex.getMessage(), ex);
            throw new IllegalStateException("Cannot calculate schema fingerprint", ex);
        }
    }
}

package com.mytransitgps.persistence.routing;

import java.sql.Connection;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：MyBatis 城市 Schema SQL 拦截器。
 *
 * 功能：将现有冻结实体产生的精确 jb.表名替换为线程上下文中的白名单 Schema；
 * 输入为 MyBatis 已生成 SQL，输出为显式 Schema-qualified SQL；不修改 core/public/jbsp，也不改变数据库 search_path。
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
public class CitySchemaSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(CitySchemaSqlInterceptor.class);
    private static final Pattern JB_SCHEMA_PREFIX = Pattern.compile("(?i)(?<![A-Za-z0-9_])jb\\.");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String schema = CitySchemaContext.current();
        if (schema != null && !"jb".equals(schema)) {
            // 仅替换代码生成 SQL 中精确的 jb. 前缀，不修改 core/public 或数据库 search_path。
            if (!CitySchemaRouter.ALLOWED_SCHEMAS.contains(schema)) {
                log.error("MyBatis SQL路由拒绝非法Schema，schema={}", schema);
                throw new IllegalStateException("UNSUPPORTED_SCHEMA: " + schema);
            }
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            MetaObject metaObject = SystemMetaObject.forObject(handler);
            BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
            if (boundSql == null) {
                boundSql = handler.getBoundSql();
            }
            MetaObject boundMeta = SystemMetaObject.forObject(boundSql);
            boundMeta.setValue("sql", routeSql(boundSql.getSql(), schema));
        }
        return invocation.proceed();
    }

    String routeSql(String sql, String schema) {
        if (!CitySchemaRouter.ALLOWED_SCHEMAS.contains(schema)) {
            log.error("SQL前缀改写拒绝非法Schema，schema={}", schema);
            throw new IllegalArgumentException("UNSUPPORTED_SCHEMA: " + schema);
        }
        return JB_SCHEMA_PREFIX.matcher(sql).replaceAll(schema + ".");
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无动态属性，避免外部配置扩大 Schema 范围。
    }
}

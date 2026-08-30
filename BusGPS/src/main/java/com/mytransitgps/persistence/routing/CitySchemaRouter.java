package com.mytransitgps.persistence.routing;

import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：城市数据库 Schema 安全路由器。
 *
 * 功能：把数据库解析出的城市 Schema 限定在固定白名单，并用 try/finally 建立和清理线程上下文；
 * 输入为 core.study_city.schema_name，输出为受控数据库调用结果；责任边界是不接受任意 Schema。
 */
@Component
public class CitySchemaRouter {

    private static final Logger log = LoggerFactory.getLogger(CitySchemaRouter.class);
    public static final Set<String> ALLOWED_SCHEMAS = Set.of("jb", "kuching", "kl", "melaka");

    public String requireAllowed(String schema) {
        if (schema == null || !ALLOWED_SCHEMAS.contains(schema)) {
            log.error("拒绝非法数据库Schema路由，schema={}", schema);
            throw new IllegalArgumentException("UNSUPPORTED_SCHEMA: " + schema);
        }
        return schema;
    }

    public <T> T withSchema(String schema, Supplier<T> action) {
        // Schema 必须先通过固定白名单，再放入当前线程上下文供 MyBatis 拦截器读取。
        String allowed = requireAllowed(schema);
        String previous = CitySchemaContext.current();
        CitySchemaContext.set(allowed);
        try {
            return action.get();
        } finally {
            // 无论 SQL 成功或抛错，都恢复上一层上下文，防止线程池复用造成跨城市串库。
            if (previous == null) {
                CitySchemaContext.clear();
            } else {
                CitySchemaContext.set(previous);
            }
        }
    }

    public void withSchema(String schema, Runnable action) {
        withSchema(schema, () -> {
            action.run();
            return null;
        });
    }
}

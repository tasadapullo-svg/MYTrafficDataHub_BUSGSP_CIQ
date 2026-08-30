package com.mytransitgps.persistence.routing;

/**
 * 中文名称：城市 Schema 线程上下文。
 *
 * 功能：仅在一次受控数据库调用期间保存已经过白名单校验的 Schema；
 * 输入为路由器验证后的 Schema，输出供 MyBatis SQL 拦截器读取；责任边界是不解析任何外部输入。
 */
public final class CitySchemaContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CitySchemaContext() {
    }

    static void set(String schema) {
        CURRENT.set(schema);
    }

    public static String current() {
        return CURRENT.get();
    }

    static void clear() {
        CURRENT.remove();
    }
}

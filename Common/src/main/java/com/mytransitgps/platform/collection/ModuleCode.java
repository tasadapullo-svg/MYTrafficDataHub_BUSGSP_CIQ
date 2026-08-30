package com.mytransitgps.platform.collection;

/**
 * 交通数据平台模块代码。
 *
 * 统一标识当前及未来业务模块，避免在调度、日志和监控代码中散落字符串常量。
 */
public enum ModuleCode {
    BUS_GPS,
    CIQ,
    WEATHER,
    FCD,
    LTA,
    TOMTOM,
    HERE,
    TRAFFIC_INCIDENT,
    ROAD_NETWORK,
    AIR_QUALITY
}

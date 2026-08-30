package com.mytransitgps.platform.collection;

/** 平台采集器代码。CIQ 八接口均使用独立代码，便于日志、审计和调度隔离。 */
public enum CollectorCode {
    CIQ_TRAFFIC_SPEED,
    CIQ_EST_TRAVEL_TIMES,
    CIQ_TRAFFIC_INCIDENTS,
    CIQ_VMS,
    CIQ_FAULTY_TRAFFIC_LIGHTS,
    CIQ_ROAD_WORKS,
    CIQ_TRAFFIC_FLOW,
    CIQ_ROAD_OPENINGS
}

package com.mytransitgps.platform.collection;

/** 统一采集执行状态，用于调度日志和平台监控。 */
public enum CollectionStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    SKIPPED,
    NOT_CONFIGURED,
    FAILED
}

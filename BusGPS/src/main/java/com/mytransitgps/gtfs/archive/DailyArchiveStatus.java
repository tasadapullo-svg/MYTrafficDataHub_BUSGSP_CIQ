package com.mytransitgps.gtfs.archive;

/**
 * 每日归档状态，区分成功、带告警、无数据、已存在和失败场景。
 */
public enum DailyArchiveStatus {
    SUCCESS,
    SUCCESS_WITH_WARNINGS,
    NO_DATA,
    ALREADY_EXISTS,
    FAILED
}

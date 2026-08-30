package com.mytransitgps.persistence.model;

import java.util.Map;
import java.util.UUID;

/**
 * 中文名称：Johor Bahru Static UUID 索引。
 *
 * 功能说明：保存一个已确认 Static Version 下的源 GTFS ID 到数据库 UUID 的映射，
 * Realtime 持久化时通过该索引建立冻结 Schema 中的外键，不硬编码任何 UUID。
 */
public record StaticReferenceIndex(
        UUID staticVersionUid,
        String staticSha256,
        Map<String, UUID> routeUids,
        Map<String, UUID> tripUids,
        Map<String, UUID> stopUids,
        Map<String, UUID> shapeUids) {
}

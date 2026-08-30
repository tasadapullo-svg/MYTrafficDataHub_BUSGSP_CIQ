package com.mytransitgps.modules.ciq.domain;

import java.util.UUID;

/**
 * API01 道路 Link 与 CIQ 研究区的空间命中结果。
 *
 * <p>同一 Link 可以同时命中 Woodlands/Tuas 以及 CORE、APPROACH、CORRIDOR 多个研究区，
 * 下游以该模型生成 traffic_link_scope 多对多关系。</p>
 */
public record TrafficSpeedScopeMatch(
        UUID areaUid,
        String ciqCode,
        String zoneCode,
        String areaName
) {
    public int priority() {
        return switch (zoneCode) {
            case "CORE" -> 1;
            case "APPROACH" -> 2;
            case "CORRIDOR" -> 3;
            default -> 99;
        };
    }
}


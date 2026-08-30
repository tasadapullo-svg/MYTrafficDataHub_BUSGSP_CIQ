package com.mytransitgps.modules.ciq.domain;

import java.math.BigDecimal;

/**
 * API01 TrafficSpeedBands 单条原始业务记录。
 *
 * <p>字段严格对应 LTA TrafficSpeedBands JSON，坐标使用 BigDecimal 保留十进制度精度，
 * 不在该模型中混入 QC、空间范围或数据库主键语义。</p>
 */
public record TrafficSpeedBandRecord(
        String linkId,
        String roadName,
        Short roadCategory,
        Short speedBand,
        Short minimumSpeed,
        Short maximumSpeed,
        BigDecimal startLon,
        BigDecimal startLat,
        BigDecimal endLon,
        BigDecimal endLat
) {
}


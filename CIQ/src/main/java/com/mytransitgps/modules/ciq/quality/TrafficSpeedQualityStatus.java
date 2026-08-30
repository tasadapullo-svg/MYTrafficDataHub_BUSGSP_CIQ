package com.mytransitgps.modules.ciq.quality;

/**
 * API01 单条记录质量状态。
 *
 * <p>QC 状态只描述字段质量，不表达是否落入 Woodlands/Tuas 研究范围；
 * 合法但区域外的数据应在空间环节标记为 OUT_OF_SCOPE。</p>
 */
public enum TrafficSpeedQualityStatus {
    VALID,
    WARNING,
    REJECTED
}


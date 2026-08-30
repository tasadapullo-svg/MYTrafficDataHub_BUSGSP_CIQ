package com.mytransitgps.modules.ciq.quality;

import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import java.util.List;

/**
 * API01 单条记录 QC 结果。
 *
 * <p>保留原始解析记录、质量状态、问题编码和页内重复标记，供 collector 汇总统计和后续入库决策使用。</p>
 */
public record TrafficSpeedValidationResult(
        TrafficSpeedBandRecord record,
        TrafficSpeedQualityStatus status,
        List<String> issues,
        boolean duplicateInPage
) {
    public boolean acceptedForSpatialFilter() {
        return status != TrafficSpeedQualityStatus.REJECTED;
    }
}


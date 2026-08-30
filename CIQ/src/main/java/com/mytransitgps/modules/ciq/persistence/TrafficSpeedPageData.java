package com.mytransitgps.modules.ciq.persistence;

import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedValidationResult;
import java.util.List;

/**
 * API01 单页解析与 QC 后的数据包。
 *
 * <p>collector 以页为单位传入该对象，持久化层据此执行 PostGIS 空间过滤和批量写入，
 * 避免把完整 TrafficSpeedBands 快照一次性堆入内存。</p>
 */
public record TrafficSpeedPageData(
        int pageNo,
        int skipValue,
        List<TrafficSpeedBandRecord> records,
        List<TrafficSpeedValidationResult> validationResults
) {
}


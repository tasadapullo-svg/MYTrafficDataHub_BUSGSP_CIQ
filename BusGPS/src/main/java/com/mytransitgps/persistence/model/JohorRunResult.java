package com.mytransitgps.persistence.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 中文名称：Johor Bahru 十分钟数据库验收运行结果。
 *
 * 功能说明：汇总计划轮次请求、数据库运行标识、Static准备结果及最终报告目录。
 */
public record JohorRunResult(
        UUID runUid,
        String runCode,
        Instant startedAt,
        Instant endedAt,
        PreparedDatabaseStatic preparedStatic,
        List<JohorCycleEvidence> cycles,
        Path reportRoot,
        boolean passed,
        long fieldMismatchCount) {
}

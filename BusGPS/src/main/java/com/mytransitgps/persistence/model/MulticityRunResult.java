package com.mytransitgps.persistence.model;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 中文名称：多城市数据库运行结果；汇总 Run、请求证据及采集/验证状态。 */
public record MulticityRunResult(
        UUID runUid,
        String runCode,
        Instant startedAt,
        Instant endedAt,
        List<MulticityCycleEvidence> attempts,
        Path reportRoot,
        boolean passed,
        long fieldMismatchCount) {
}

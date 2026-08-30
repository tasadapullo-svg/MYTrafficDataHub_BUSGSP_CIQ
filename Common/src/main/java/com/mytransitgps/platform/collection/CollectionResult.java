package com.mytransitgps.platform.collection;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 单次采集统一结果。
 *
 * 计数字段为平台级统计，不要求稳定BUS GPS业务在第一阶段迁移或补填。
 */
public record CollectionResult(
        UUID runUid,
        ModuleCode moduleCode,
        CollectorCode collectorCode,
        boolean success,
        CollectionStatus status,
        Instant startTime,
        Instant endTime,
        long durationMs,
        int requestCount,
        long receivedCount,
        long validCount,
        long duplicateCount,
        long insertedCount,
        long updatedCount,
        long rejectedCount,
        long errorCount,
        Integer httpStatus,
        String errorCode,
        String errorMessage
) {
    public CollectionResult {
        Objects.requireNonNull(runUid, "runUid");
        Objects.requireNonNull(moduleCode, "moduleCode");
        Objects.requireNonNull(collectorCode, "collectorCode");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
    }

    /** 创建尚未配置或被安全跳过的采集结果。 */
    public static CollectionResult skipped(CollectionContext context, CollectionStatus status,
                                           Instant startTime, String errorCode, String errorMessage) {
        Instant endTime = Instant.now();
        return new CollectionResult(
                context.runUid(), context.moduleCode(), context.collectorCode(), false, status,
                startTime, endTime, Duration.between(startTime, endTime).toMillis(),
                0, 0, 0, 0, 0, 0, 0, 0, null, errorCode, errorMessage);
    }

    /** 创建平台异常边界捕获后的失败结果。 */
    public static CollectionResult failed(CollectionContext context, Instant startTime, Throwable error) {
        Instant endTime = Instant.now();
        return new CollectionResult(
                context.runUid(), context.moduleCode(), context.collectorCode(), false, CollectionStatus.FAILED,
                startTime, endTime, Duration.between(startTime, endTime).toMillis(),
                0, 0, 0, 0, 0, 0, 0, 1, null,
                error.getClass().getSimpleName(), safeMessage(error));
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.isBlank() ? "Collector execution failed" : message;
    }
}

package com.mytransitgps.platform.collection;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 单次采集执行上下文。
 *
 * 上下文只携带平台级追踪信息，不包含任何具体API返回字段或数据库业务模型。
 */
public record CollectionContext(
        UUID runUid,
        Instant triggerTime,
        Instant scheduledTime,
        ModuleCode moduleCode,
        CollectorCode collectorCode,
        String traceId,
        boolean manualTrigger,
        Map<String, String> metadata
) {
    public CollectionContext {
        Objects.requireNonNull(runUid, "runUid");
        Objects.requireNonNull(triggerTime, "triggerTime");
        Objects.requireNonNull(moduleCode, "moduleCode");
        Objects.requireNonNull(collectorCode, "collectorCode");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /** 创建一次由调度器触发的标准上下文。 */
    public static CollectionContext scheduled(ModuleCode moduleCode, CollectorCode collectorCode, Instant scheduledTime) {
        Instant now = Instant.now();
        return new CollectionContext(
                UUID.randomUUID(), now, scheduledTime, moduleCode, collectorCode,
                UUID.randomUUID().toString(), false, Map.of());
    }
}

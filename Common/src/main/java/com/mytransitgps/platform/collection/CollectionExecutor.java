package com.mytransitgps.platform.collection;

import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 平台统一采集执行器。
 *
 * 为每个Collector建立独立异常边界，集中记录开始、完成和失败摘要，避免单个模块异常
 * 传播到BUS GPS长期线程、Spring容器或Dashboard。
 */
@Component
public class CollectionExecutor {
    private static final Logger log = LoggerFactory.getLogger(CollectionExecutor.class);

    /** 在统一异常边界内执行一次采集。 */
    public CollectionResult execute(DataCollector collector, CollectionContext context) {
        Objects.requireNonNull(collector, "collector");
        Objects.requireNonNull(context, "context");
        if (collector.moduleCode() != context.moduleCode()
                || collector.collectorCode() != context.collectorCode()) {
            throw new IllegalArgumentException("Collector与CollectionContext代码不一致");
        }

        Instant startedAt = Instant.now();
        log.info("交通数据采集开始，module={}，collector={}，runUid={}，scheduledTime={}",
                collector.moduleCode(), collector.collectorCode(), context.runUid(), context.scheduledTime());
        try {
            CollectionResult result = Objects.requireNonNull(collector.collect(context), "CollectionResult");
            if (result.success()) {
                log.info("交通数据采集完成，module={}，collector={}，runUid={}，received={}，valid={}，inserted={}，updated={}，durationMs={}",
                        result.moduleCode(), result.collectorCode(), result.runUid(), result.receivedCount(),
                        result.validCount(), result.insertedCount(), result.updatedCount(), result.durationMs());
            } else {
                log.warn("交通数据采集未成功，module={}，collector={}，runUid={}，status={}，errorCode={}，message={}",
                        result.moduleCode(), result.collectorCode(), result.runUid(), result.status(),
                        result.errorCode(), result.errorMessage());
            }
            return result;
        } catch (RuntimeException error) {
            CollectionResult failed = CollectionResult.failed(context, startedAt, error);
            log.error("交通数据采集失败，module={}，collector={}，runUid={}，errorType={}，message={}",
                    context.moduleCode(), context.collectorCode(), context.runUid(),
                    error.getClass().getSimpleName(), failed.errorMessage(), error);
            return failed;
        }
    }
}

package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.modules.ciq.scheduler.CiqCollectionScheduler;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionExecutor;
import com.mytransitgps.platform.collection.CollectionResult;
import com.mytransitgps.platform.collection.DataCollector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** 验证 API01 Scheduler 单 JVM 防重入。 */
class SchedulerReentryTest {
    @Test
    void skipsSecondTriggerWhileFirstRunIsActive() {
        AtomicInteger executions = new AtomicInteger();
        AtomicReference<CiqCollectionScheduler> schedulerRef = new AtomicReference<>();
        CollectionExecutor executor = new CollectionExecutor() {
            @Override
            public CollectionResult execute(DataCollector collector, CollectionContext context) {
                executions.incrementAndGet();
                schedulerRef.get().collectTrafficSpeed();
                return CollectionResult.skipped(context, com.mytransitgps.platform.collection.CollectionStatus.SKIPPED,
                        context.triggerTime(), "TEST", "test");
            }
        };
        ObjectProvider<CiqTrafficSpeedCollector> provider = providerOf(mock(CiqTrafficSpeedCollector.class));
        CiqCollectionScheduler scheduler = new CiqCollectionScheduler(executor, provider);
        schedulerRef.set(scheduler);

        scheduler.collectTrafficSpeed();

        assertEquals(1, executions.get());
    }

    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) { return value; }
            @Override
            public T getIfAvailable() { return value; }
            @Override
            public T getIfUnique() { return value; }
            @Override
            public T getObject() { return value; }
        };
    }
}


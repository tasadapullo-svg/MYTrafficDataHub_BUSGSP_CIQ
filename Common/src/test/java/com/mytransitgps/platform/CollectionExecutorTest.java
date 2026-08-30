package com.mytransitgps.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionExecutor;
import com.mytransitgps.platform.collection.CollectionStatus;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.DataCollector;
import com.mytransitgps.platform.collection.ModuleCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CollectionExecutorTest {

    @Test
    void collectorFailureMustStayInsideModuleBoundary() {
        DataCollector failingCollector = new DataCollector() {
            @Override public ModuleCode moduleCode() { return ModuleCode.CIQ; }
            @Override public CollectorCode collectorCode() { return CollectorCode.CIQ_TRAFFIC_SPEED; }
            @Override public com.mytransitgps.platform.collection.CollectionResult collect(CollectionContext context) {
                throw new IllegalStateException("isolated failure");
            }
        };
        CollectionContext context = CollectionContext.scheduled(
                ModuleCode.CIQ, CollectorCode.CIQ_TRAFFIC_SPEED, Instant.now());

        var result = new CollectionExecutor().execute(failingCollector, context);

        assertFalse(result.success());
        assertEquals(CollectionStatus.FAILED, result.status());
        assertEquals("IllegalStateException", result.errorCode());
    }
}

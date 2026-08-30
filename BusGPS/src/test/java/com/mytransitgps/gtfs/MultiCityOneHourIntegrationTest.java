package com.mytransitgps.gtfs;

import java.nio.file.Files;

import com.mytransitgps.gtfs.model.ShortRunResult;
import com.mytransitgps.gtfs.service.MultiCityShortRunRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("one-hour")
@EnabledIfSystemProperty(named = "runOneHour", matches = "true")
class MultiCityOneHourIntegrationTest {

    @Test
    void shouldRunOneHourCollectionAgainstRealApis() throws Exception {
        MultiCityShortRunRunner runner = MultiCityShortRunRunner.oneHourRunner(TestArtifactSupport.workspaceRoot());

        ShortRunResult result = runner.runShortRun();

        assertEquals(150, result.actualRealtimeRequests());
        assertEquals(150, result.successfulRequests() + result.failedRequests());
        assertTrue(Files.exists(result.manifestPath()));
        assertTrue(Files.exists(result.feedSummaryCsvPath()));
        assertTrue(Files.exists(result.snapshotComparisonCsvPath()));
        assertTrue(Files.exists(result.fieldPresenceJsonPath()));
        assertTrue(Files.exists(result.fieldPresenceCsvPath()));
    }
}

package com.mytransitgps.gtfs;

import java.nio.file.Files;

import com.mytransitgps.gtfs.model.ShortRunResult;
import com.mytransitgps.gtfs.service.MultiCityShortRunRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("thirty-minute-run")
@EnabledIfSystemProperty(named = "runThirtyMinuteCollection", matches = "true")
class ThirtyMinuteCollectionIntegrationTest {

    @Test
    void shouldRunThirtyMinuteCollectionAgainstRealApis() throws Exception {
        MultiCityShortRunRunner runner = new MultiCityShortRunRunner(TestArtifactSupport.workspaceRoot(), 15, 120, 24);

        ShortRunResult result = runner.runShortRun();

        assertEquals(75, result.actualRealtimeRequests());
        assertEquals(75, result.successfulRequests() + result.failedRequests());
        assertTrue(Files.exists(result.manifestPath()));
        assertTrue(Files.exists(result.feedSummaryCsvPath()));
        assertTrue(Files.exists(result.snapshotComparisonCsvPath()));
        assertTrue(Files.exists(result.fieldPresenceJsonPath()));
        assertTrue(Files.exists(result.fieldPresenceCsvPath()));
    }
}

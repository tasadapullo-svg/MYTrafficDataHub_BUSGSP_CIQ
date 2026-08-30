package com.mytransitgps.gtfs;

import java.nio.file.Files;

import com.mytransitgps.gtfs.model.ShortRunResult;
import com.mytransitgps.gtfs.service.MultiCityShortRunRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("short-run")
@EnabledIfSystemProperty(named = "runShortRun", matches = "true")
class MultiCityShortRunIntegrationTest {

    @Test
    void shouldRunTenMinuteShortRunAgainstRealApis() throws Exception {
        MultiCityShortRunRunner runner = new MultiCityShortRunRunner(TestArtifactSupport.workspaceRoot());

        ShortRunResult result = runner.runShortRun();

        assertEquals(25, result.actualRealtimeRequests());
        assertEquals(25, result.successfulRequests() + result.failedRequests());
        assertTrue(Files.exists(result.manifestPath()));
        assertTrue(Files.exists(result.feedSummaryCsvPath()));
        assertTrue(Files.exists(result.snapshotComparisonCsvPath()));
        assertTrue(Files.exists(result.fieldPresenceJsonPath()));
        assertTrue(Files.exists(result.fieldPresenceCsvPath()));
    }
}

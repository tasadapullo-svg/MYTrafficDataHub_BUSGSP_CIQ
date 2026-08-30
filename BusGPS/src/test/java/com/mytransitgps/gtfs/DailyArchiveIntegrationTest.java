package com.mytransitgps.gtfs;

import java.nio.file.Files;
import java.time.LocalDate;
import java.util.zip.ZipFile;

import com.mytransitgps.gtfs.archive.DailyArchiveResult;
import com.mytransitgps.gtfs.archive.DailyArchiveService;
import com.mytransitgps.gtfs.archive.DailyArchiveStatus;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "runArchiveIntegration", matches = "true")
class DailyArchiveIntegrationTest {

    @Test
    void shouldArchiveCurrentTestDayFromLocalArtifactsOnly() throws Exception {
        DailyArchiveService service = new DailyArchiveService(TestArtifactSupport.workspaceRoot());
        LocalDate archiveDate = LocalDate.of(2026, 8, 27);

        DailyArchiveResult result = service.archiveDate(archiveDate, true, true);

        assertTrue(result.status() == DailyArchiveStatus.SUCCESS || result.status() == DailyArchiveStatus.SUCCESS_WITH_WARNINGS);
        assertNotNull(result.archivePath());
        assertTrue(Files.exists(result.archivePath()));
        assertTrue(Files.exists(java.nio.file.Path.of(result.archivePath().toString() + ".sha256")));
        String expectedSha = Files.readString(java.nio.file.Path.of(result.archivePath().toString() + ".sha256")).split("  ")[0].trim();
        String actualSha = HashUtils.sha256Hex(Files.readAllBytes(result.archivePath()));
        assertTrue(expectedSha.equals(actualSha));
        try (ZipFile zipFile = new ZipFile(result.archivePath().toFile())) {
            assertTrue(zipFile.size() > 0);
            assertNotNull(zipFile.getEntry("MYTransitGPS_20260827/manifest/daily_manifest.json"));
            assertNotNull(zipFile.getEntry("MYTransitGPS_20260827/manifest/checksums.sha256"));
        }
    }
}

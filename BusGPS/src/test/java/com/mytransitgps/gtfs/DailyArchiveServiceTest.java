package com.mytransitgps.gtfs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.zip.ZipFile;

import com.mytransitgps.gtfs.archive.DailyArchiveResult;
import com.mytransitgps.gtfs.archive.DailyArchiveService;
import com.mytransitgps.gtfs.archive.DailyArchiveStatus;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyArchiveServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnNoDataWhenDateHasNoArtifacts() {
        DailyArchiveResult result = new DailyArchiveService(tempDir).archiveDate(LocalDate.of(2026, 8, 27), false, false);
        assertEquals(DailyArchiveStatus.NO_DATA, result.status());
    }

    @Test
    void shouldArchiveSingleDateWithWarningsAndDeduplicateStaticObjects() throws Exception {
        Path staticObject = tempDir.resolve("raw_data/static/objects/ab/cd/testsha.zip");
        Files.createDirectories(staticObject.getParent());
        Files.write(staticObject, "staticzip".getBytes(StandardCharsets.UTF_8));
        writeDayArtifacts("20260827", "Johor_Bahru", "mybas-johor", staticObject, "one_hour_run_x");
        writeDayArtifacts("20260827", "Kuala_Lumpur", "rapid-bus-kl", staticObject, "one_hour_run_x");
        Files.writeString(tempDir.resolve("BusGPS/20260827/Johor_Bahru/ignore.tmp"), "tmp");

        DailyArchiveService service = new DailyArchiveService(tempDir);
        DailyArchiveResult result = service.archiveDate(LocalDate.of(2026, 8, 27), true, false);

        assertEquals(DailyArchiveStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals(2, result.realtimeMetadataCount());
        assertEquals(2, result.staticMetadataCount());
        assertEquals(1, result.staticObjectCount());
        assertNotNull(result.archivePath());
        assertTrue(Files.exists(result.archivePath()));
        assertTrue(Files.exists(Path.of(result.archivePath().toString() + ".sha256")));

        String expectedSha = Files.readString(Path.of(result.archivePath().toString() + ".sha256")).split("  ")[0].trim();
        String actualSha = HashUtils.sha256Hex(Files.readAllBytes(result.archivePath()));
        assertEquals(expectedSha, actualSha);

        try (ZipFile zipFile = new ZipFile(result.archivePath().toFile())) {
            assertNotNull(zipFile.getEntry("MYTransitGPS_20260827/manifest/daily_manifest.json"));
            assertNotNull(zipFile.getEntry("MYTransitGPS_20260827/manifest/checksums.sha256"));
            assertNotNull(zipFile.getEntry("MYTransitGPS_20260827/README.txt"));
            assertTrue(zipFile.stream().noneMatch(entry -> entry.getName().endsWith(".tmp")));
        }
        assertFalse(Files.exists(tempDir.resolve("BusGPS/20260827/Johor_Bahru/mybas-johor_gtfs_realtime_metadata_20260827_000000.json")));
        assertFalse(Files.exists(tempDir.resolve("raw_data/20260827/Johor_Bahru/mybas-johor.pb")));
        assertFalse(Files.exists(tempDir.resolve("run_reports/20260827/one_hour_run_x/one_hour_manifest.json")));
        assertTrue(Files.exists(tempDir.resolve("BusGPS/20260827/Johor_Bahru/ignore.tmp")));
        assertTrue(Files.exists(staticObject));
    }

    @Test
    void shouldReturnAlreadyExistsUnlessForceRebuild() throws Exception {
        Path archiveDir = tempDir.resolve("BusGPS/archive/2026/08");
        Files.createDirectories(archiveDir);
        Files.writeString(archiveDir.resolve("mytransitgps_daily_20260827.zip"), "zip");

        DailyArchiveResult result = new DailyArchiveService(tempDir).archiveDate(LocalDate.of(2026, 8, 27), false, false);

        assertEquals(DailyArchiveStatus.ALREADY_EXISTS, result.status());
    }

    private void writeDayArtifacts(String date, String cityFolder, String feedId, Path staticObject, String runId) throws Exception {
        Path jsonDir = tempDir.resolve("BusGPS").resolve(date).resolve(cityFolder);
        Path rawDir = tempDir.resolve("raw_data").resolve(date).resolve(cityFolder);
        Path reportDir = tempDir.resolve("run_reports").resolve(date).resolve(runId);
        Files.createDirectories(jsonDir);
        Files.createDirectories(rawDir);
        Files.createDirectories(reportDir);
        Files.writeString(jsonDir.resolve(feedId + "_gtfs_realtime_parsed_full_" + date + "_000000.json"), "{\"feed_id\":\"" + feedId + "\"}");
        Files.writeString(jsonDir.resolve(feedId + "_gtfs_realtime_enriched_full_" + date + "_000000.json"), "{\"snapshot\":{},\"vehicles\":[]}");
        Files.writeString(jsonDir.resolve(feedId + "_gtfs_realtime_metadata_" + date + "_000000.json"),
                "{\n  \"feed_id\":\"" + feedId + "\",\n  \"run_id\":\"" + runId + "\",\n  \"request_started_at\":\"2026-08-27T00:00:00Z\",\n  \"response_received_at\":\"2026-08-27T00:00:10Z\",\n  \"result\":\"SUCCESS\",\n  \"raw_pb_path\":\"raw_data/" + date + "/" + cityFolder + "/" + feedId + ".pb\",\n  \"parsed_json_path\":\"BusGPS/" + date + "/" + cityFolder + "/" + feedId + "_gtfs_realtime_parsed_full_" + date + "_000000.json\",\n  \"enriched_json_path\":\"BusGPS/" + date + "/" + cityFolder + "/" + feedId + "_gtfs_realtime_enriched_full_" + date + "_000000.json\"\n}");
        Files.writeString(jsonDir.resolve(feedId + "_gtfs_static_metadata_" + date + "_000000.json"),
                "{\n  \"feed_id\":\"" + feedId + "\",\n  \"run_id\":\"" + runId + "\",\n  \"request_started_at\":\"2026-08-27T00:00:00Z\",\n  \"response_received_at\":\"2026-08-27T00:00:10Z\",\n  \"static_object_path\":\"" + tempDir.relativize(staticObject).toString().replace('\\', '/') + "\"\n}");
        Files.write(rawDir.resolve(feedId + ".pb"), "protobuf".getBytes(StandardCharsets.UTF_8));
        Files.writeString(reportDir.resolve("one_hour_manifest.json"), "{\"run_id\":\"" + runId + "\"}");
    }
}

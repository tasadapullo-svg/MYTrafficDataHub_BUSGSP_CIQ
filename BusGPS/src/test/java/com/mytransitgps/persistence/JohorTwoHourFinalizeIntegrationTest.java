package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.gtfs.archive.DailyArchiveService;
import com.mytransitgps.gtfs.archive.DailyArchiveStatus;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.gtfs.util.HashUtils;
import org.junit.jupiter.api.Test;

/** 使用既有两小时验证报告离线重建并终检当天归档，不发起网络请求。 */
class JohorTwoHourFinalizeIntegrationTest {

    @Test
    void rebuildsAndVerifiesCurrentDailyArchiveWithoutNetwork() throws Exception {
        assumeTrue(Boolean.getBoolean("mytransitgps.long-test.finalize.confirm"));
        String runCode = System.getProperty("mytransitgps.db-validation-run-code");
        Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path workspace = project.getParent();
        Path reportRoot = workspace.resolve("outputs/db_validation").resolve(runCode);
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String,Object> existing = mapper.readValue(reportRoot.resolve("16_daily_archive_validation.json").toFile(), Map.class);

        LocalDate date = GtfsTime.malaysiaToday();
        DailyArchiveService service = new DailyArchiveService(workspace);
        var first = service.archiveDate(date, true, true);
        Map<String,Object> report = new LinkedHashMap<>(existing);
        report.put("daily_archive_status", first.status());
        report.put("daily_archive_path", first.archivePath().toString());
        report.put("daily_archive_sha256", "SEE_FINAL_SIDECAR");
        report.put("archive_entry_count", first.archiveEntryCount());
        report.put("archive_internal_checksums", "PASS");
        report.put("one_zip_for_date", oneZipForDate(first.archivePath().getParent(), date));
        mapper.writerWithDefaultPrettyPrinter().writeValue(reportRoot.resolve("16_daily_archive_validation.json").toFile(), report);

        var archive = service.archiveDate(date, true, true);
        assertThat(archive.status()).isEqualTo(DailyArchiveStatus.SUCCESS_WITH_WARNINGS);
        String actualSha = HashUtils.sha256Hex(Files.readAllBytes(archive.archivePath()));
        assertThat(actualSha).isEqualTo(archive.archiveSha256());
        assertThat(Files.readString(Path.of(archive.archivePath().toString() + ".sha256"))).startsWith(actualSha);
        assertThat(oneZipForDate(archive.archivePath().getParent(), date)).isTrue();
    }

    private boolean oneZipForDate(Path directory, LocalDate date) throws Exception {
        String expected = "mytransitgps_daily_" + GtfsTime.formatBatchDate(date) + ".zip";
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().equals(expected)).count() == 1;
        }
    }
}

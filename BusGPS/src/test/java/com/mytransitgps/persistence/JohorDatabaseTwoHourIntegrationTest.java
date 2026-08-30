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
import com.mytransitgps.persistence.service.JohorDatabaseTenMinuteRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Johor Bahru 两小时生产式验收：60轮绝对120秒调度，完成后验证磁盘JSON、数据库、日志和每日ZIP。
 */
@Tag("db-2h")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "mytransitgps.long-test.enabled=true", "app.redis.connect-on-startup=false"})
class JohorDatabaseTwoHourIntegrationTest {
    @Autowired JohorDatabaseTenMinuteRunner runner;
    @Autowired ObjectMapper objectMapper;

    @Test void runsTwoHoursThenValidatesDatabaseJsonLogsAndDailyArchive() throws Exception {
        assumeTrue(Boolean.getBoolean("mytransitgps.long-test.confirm"), "需要显式确认两小时真实采集");
        Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath(); Path workspace = project.getParent();
        var run = runner.runTwoHour(workspace);
        assertThat(run.cycles()).hasSize(60); assertThat(run.fieldMismatchCount()).isZero(); assertThat(run.passed()).isTrue();

        LocalDate date = GtfsTime.malaysiaToday();
        DailyArchiveService archiveService = new DailyArchiveService(workspace);
        var archive = archiveService.archiveDate(date, true, true);
        assertThat(archive.status()).isEqualTo(DailyArchiveStatus.SUCCESS_WITH_WARNINGS);
        assertThat(archive.realtimeMetadataCount()).isGreaterThanOrEqualTo(60);
        assertThat(archive.rawPbCount()).isGreaterThanOrEqualTo(60);
        assertThat(archive.parsedJsonCount()).isGreaterThanOrEqualTo(60);
        assertThat(archive.enrichedJsonCount()).isGreaterThanOrEqualTo(60);
        assertThat(HashUtils.sha256Hex(Files.readAllBytes(archive.archivePath()))).isEqualTo(archive.archiveSha256());

        Map<String,Object> report = new LinkedHashMap<>();
        report.put("run_code", run.runCode()); report.put("run_uid", run.runUid()); report.put("cycles", run.cycles().size());
        report.put("json_database_validation", "PASS"); report.put("field_mismatches", run.fieldMismatchCount());
        report.put("info_log_exact_level", exactLevel(project.resolve("logs/info/mytransitgps-info.log"), "INFO"));
        report.put("warn_log_exact_level", exactLevel(project.resolve("logs/warn/mytransitgps-warn.log"), "WARN"));
        report.put("error_log_exact_level", exactLevel(project.resolve("logs/error/mytransitgps-error.log"), "ERROR"));
        report.put("daily_archive_status", archive.status()); report.put("daily_archive_path", archive.archivePath().toString());
        report.put("daily_archive_sha256", "SEE_FINAL_SIDECAR"); report.put("archive_entry_count", archive.archiveEntryCount());
        report.put("archive_internal_checksums", "PASS"); report.put("one_zip_for_date", oneZipForDate(archive.archivePath().getParent(), date));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(run.reportRoot().resolve("16_daily_archive_validation.json").toFile(), report);

        var finalArchive = archiveService.archiveDate(date, true, true);
        assertThat(finalArchive.status()).isEqualTo(DailyArchiveStatus.SUCCESS_WITH_WARNINGS);
        assertThat(HashUtils.sha256Hex(Files.readAllBytes(finalArchive.archivePath()))).isEqualTo(finalArchive.archiveSha256());
        assertThat(Files.readString(Path.of(finalArchive.archivePath().toString() + ".sha256")))
                .startsWith(finalArchive.archiveSha256());
        assertThat(oneZipForDate(finalArchive.archivePath().getParent(), date)).isTrue();
    }

    private boolean exactLevel(Path path, String expected) throws Exception {
        if (!Files.isRegularFile(path)) return false;
        for (String line : Files.readAllLines(path)) {
            var matcher = java.util.regex.Pattern.compile("\\] (INFO|WARN|ERROR) ").matcher(line);
            if (matcher.find() && !expected.equals(matcher.group(1))) return false;
        }
        return true;
    }

    private boolean oneZipForDate(Path directory, LocalDate date) throws Exception {
        String expected = "mytransitgps_daily_" + GtfsTime.formatBatchDate(date) + ".zip";
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().equals(expected)).count() == 1;
        }
    }
}

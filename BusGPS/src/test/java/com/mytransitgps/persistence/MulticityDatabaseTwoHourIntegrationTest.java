package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.mytransitgps.gtfs.archive.DailyArchiveService;
import com.mytransitgps.gtfs.archive.DailyArchiveStatus;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.persistence.model.MulticityRunResult;
import com.mytransitgps.persistence.service.MulticityDatabaseTenMinuteRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 仅供显式执行的四城市五 Feed、每 Feed 六十轮、120 秒间隔真实数据库验收测试。 */
@Tag("multicity-db-2h")
@SpringBootTest(properties = {
        "mytransitgps.database.enabled=true",
        "mytransitgps.multicity-db-test.enabled=true",
        "mytransitgps.multicity-db-test.duration-minutes=120",
        "mytransitgps.multicity-db-test.cycles-per-feed=60",
        "mytransitgps.multicity-db-test.interval-seconds=120",
        "app.redis.connect-on-startup=false"
})
class MulticityDatabaseTwoHourIntegrationTest {
    @Autowired MulticityDatabaseTenMinuteRunner runner;

    @Test void runsThreeHundredRealAttemptsFromDiskJsonIntoCitySchemas() throws Exception {
        assumeTrue(Boolean.getBoolean("mytransitgps.multicity-db-2h.confirm"),
                "需要 -Dmytransitgps.multicity-db-2h.confirm=true 显式确认");
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        MulticityRunResult result = runner.runTwoHour(workspace, true);
        assertThat(result.attempts()).hasSize(300);
        assertThat(result.attempts()).allMatch(attempt -> attempt.persistenceResult() != null);
        assertThat(result.passed()).isTrue();

        var archive = new DailyArchiveService(workspace).archiveDate(GtfsTime.malaysiaToday(), true, true);
        assertThat(archive.status()).isIn(DailyArchiveStatus.SUCCESS, DailyArchiveStatus.SUCCESS_WITH_WARNINGS);
        assertThat(HashUtils.sha256Hex(Files.readAllBytes(archive.archivePath())))
                .isEqualTo(archive.archiveSha256());
        assertThat(Files.readString(Path.of(archive.archivePath().toString() + ".sha256")))
                .startsWith(archive.archiveSha256());
        assertThat(result.attempts()).allSatisfy(attempt -> {
            assertThat(zipContains(workspace, archive.archivePath(), attempt.rawPath())).isTrue();
            assertThat(zipContains(workspace, archive.archivePath(), attempt.parsedPath())).isTrue();
            assertThat(zipContains(workspace, archive.archivePath(), attempt.enrichedPath())).isTrue();
        });
        try (var files = Files.list(archive.archivePath().getParent())) {
            String expected = "mytransitgps_daily_" + GtfsTime.formatBatchDate(GtfsTime.malaysiaToday()) + ".zip";
            assertThat(files.filter(path -> path.getFileName().toString().equals(expected)).count()).isOne();
        }
    }

    private boolean zipContains(Path workspace, Path archive, Path source) {
        String date = GtfsTime.formatBatchDate(GtfsTime.malaysiaToday());
        String entry = "MYTransitGPS_" + date + "/"
                + workspace.relativize(source).toString().replace('\\', '/');
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            return zip.getEntry(entry) != null;
        } catch (Exception ex) {
            throw new IllegalStateException("无法检查日归档条目: " + entry, ex);
        }
    }
}

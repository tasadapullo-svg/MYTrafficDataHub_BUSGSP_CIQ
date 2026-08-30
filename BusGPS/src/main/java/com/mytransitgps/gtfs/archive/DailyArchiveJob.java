package com.mytransitgps.gtfs.archive;

import java.time.LocalDate;
import java.time.ZoneId;

import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日归档定时任务，在马来西亚时区归档并校验前一天的完整数据。
 */
@Component
@ConditionalOnProperty(prefix = "archive.daily", name = "enabled", havingValue = "true")
public class DailyArchiveJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyArchiveJob.class);

    private final DailyArchiveProperties properties;
    private final WorkspaceRootResolver workspaceRootResolver;

    public DailyArchiveJob(DailyArchiveProperties properties, WorkspaceRootResolver workspaceRootResolver) {
        this.properties = properties;
        this.workspaceRootResolver = workspaceRootResolver;
    }

    @Scheduled(cron = "${archive.daily.cron:0 30 0 * * *}", zone = "${archive.daily.zone:Asia/Kuala_Lumpur}")
    public void archiveYesterday() {
        // 每次重新解析工作区根目录，避免 IDEA 与命令行工作目录不同导致归档错位。
        DailyArchiveService service = new DailyArchiveService(workspaceRootResolver.resolve());
        LocalDate yesterday = LocalDate.now(ZoneId.of(properties.getZone())).minusDays(1);
        LOGGER.info("开始执行每日归档任务，archiveDate={}，zone={}", yesterday, properties.getZone());
        // 强制原子重建同名昨日 ZIP，确保早期生成的日内旧包不会遗漏后续数据。
        DailyArchiveResult result = service.archiveDate(yesterday, false, true);
        if (result.status() == DailyArchiveStatus.FAILED) {
            LOGGER.error("每日归档任务失败，archiveDate={}，status={}，error={}",
                    result.archiveDate(), result.status(), result.errorMessage());
        } else if (result.status() == DailyArchiveStatus.SUCCESS_WITH_WARNINGS || result.status() == DailyArchiveStatus.NO_DATA) {
            LOGGER.warn("每日归档任务完成但存在提示，archiveDate={}，status={}，sourceFiles={}，entries={}，warnings={}，archive={}",
                    result.archiveDate(), result.status(), result.sourceFileCount(), result.archiveEntryCount(), result.warningMessages(), result.archivePath());
        } else {
            LOGGER.info("每日归档任务完成，archiveDate={}，status={}，sourceFiles={}，entries={}，archive={}",
                    result.archiveDate(), result.status(), result.sourceFileCount(), result.archiveEntryCount(), result.archivePath());
        }
    }
}

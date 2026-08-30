package com.mytransitgps.modules.ciq.archive;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CIQ 每日 JSON 归档定时任务。
 *
 * <p>在马来西亚时区每天 00:30 归档前一天 {@code CIQ/yyyyMMdd} 下八个接口目录，
 * ZIP 校验成功后删除源日期目录；失败时保留源文件。
 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "traffic.ciq.archive", name = "enabled", havingValue = "true")
public class CiqDailyArchiveJob {
    private static final Logger log = LoggerFactory.getLogger(CiqDailyArchiveJob.class);

    private final CiqDailyArchiveService archiveService;
    private final CiqProperties properties;

    public CiqDailyArchiveJob(CiqDailyArchiveService archiveService, CiqProperties properties) {
        this.archiveService = archiveService;
        this.properties = properties;
    }

    @Scheduled(cron = "${traffic.ciq.archive.cron:0 30 0 * * *}",
            zone = "${traffic.ciq.archive.zone:Asia/Kuala_Lumpur}")
    public void archiveYesterday() {
        String zone = properties.getArchive().getZone();
        LocalDate yesterday = LocalDate.now(ZoneId.of(zone)).minusDays(1);
        log.info("CIQ每日归档定时任务开始，archiveDate={}，zone={}", yesterday, zone);
        CiqDailyArchiveResult result = archiveService.archiveDate(yesterday, true);
        switch (result.status()) {
            case SUCCESS -> log.info("CIQ每日归档定时任务完成，archiveDate={}，sourceFiles={}，archive={}",
                    result.archiveDate(), result.sourceFileCount(), result.archivePath());
            case NO_DATA, ALREADY_EXISTS -> log.warn("CIQ每日归档定时任务无新增归档，archiveDate={}，status={}，archive={}",
                    result.archiveDate(), result.status(), result.archivePath());
            case FAILED -> log.error("CIQ每日归档定时任务失败，archiveDate={}，error={}",
                    result.archiveDate(), result.errorMessage());
        }
    }
}

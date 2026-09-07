package com.mytransitgps.modules.ciqbus.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class CiqBusDailyExportScheduler {
    private static final Logger log = LoggerFactory.getLogger(CiqBusDailyExportScheduler.class);
    private final CiqBusDailyExportService service;

    public CiqBusDailyExportScheduler(CiqBusDailyExportService service) {
        this.service = service;
    }

    @Scheduled(cron = "${ciqbus.daily-export.cron:0 10 0 * * *}", zone = "${ciqbus.timezone:Asia/Kuala_Lumpur}")
    public void exportPreviousDay() {
        log.info("[CIQBUS-XLSX] 项目进度：开始执行CIQBus每日Excel导出");
        log.info("[CIQBUS-XLSX] 项目进度：CIQBus每日Excel导出完成，output={}", service.exportPreviousDay(Instant.now()));
    }
}

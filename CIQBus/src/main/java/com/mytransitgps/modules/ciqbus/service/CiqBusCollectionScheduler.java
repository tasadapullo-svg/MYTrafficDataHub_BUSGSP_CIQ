package com.mytransitgps.modules.ciqbus.service;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class CiqBusCollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CiqBusCollectionScheduler.class);

    private final CiqBusRealtimeService service;
    private final AtomicBoolean collecting = new AtomicBoolean();

    public CiqBusCollectionScheduler(CiqBusRealtimeService service) {
        this.service = service;
    }

    @Scheduled(cron = "${ciqbus.scheduler.cron:0 */2 * * * *}", zone = "${ciqbus.timezone:Asia/Kuala_Lumpur}")
    public void runCycle() {
        if (!collecting.compareAndSet(false, true)) {
            log.warn("[CIQBUS-LTA] 项目进度：上一轮CIQBus采集仍在运行，本次定时触发跳过");
            return;
        }
        String cycleId = UUID.randomUUID().toString();
        try {
            log.info("[CIQBUS-LTA] 项目进度：CIQBus定时采集开始，cycleId={}", cycleId);
            service.runCycle(cycleId);
            log.info("[CIQBUS-LTA] 项目进度：CIQBus定时采集结束，cycleId={}", cycleId);
        } catch (RuntimeException ex) {
            log.error("[CIQBUS-LTA] 项目进度：CIQBus定时采集失败，cycleId={}，errorType={}，reason={}",
                    cycleId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
        } finally {
            collecting.set(false);
        }
    }

    boolean isCollecting() {
        return collecting.get();
    }
}

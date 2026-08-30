package com.mytransitgps.modules.ciq.scheduler;

import com.mytransitgps.modules.ciq.collector.CiqTrafficSpeedCollector;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionExecutor;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.ModuleCode;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * CIQ 调度器。
 *
 * <p>仅负责按配置时间触发统一执行器，并使用进程内原子锁防止同一 API01 任务重入；
 * 不包含 HTTP、JSON 解析、质量检查或数据库业务逻辑。
 */
public class CiqCollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CiqCollectionScheduler.class);

    private final CollectionExecutor executor;
    private final ObjectProvider<CiqTrafficSpeedCollector> collectorProvider;
    private final AtomicBoolean trafficSpeedRunning = new AtomicBoolean();

    public CiqCollectionScheduler(CollectionExecutor executor,
                                  ObjectProvider<CiqTrafficSpeedCollector> collectorProvider) {
        this.executor = executor;
        this.collectorProvider = collectorProvider;
    }

    /** 按每小时 00/15/30/45 分钟触发 API01；上一轮未完成时安全跳过。 */
    @Scheduled(cron = "${traffic.ciq.collectors.traffic-speed.cron:${traffic.ciq.schedule.cron}}", zone = "${traffic.ciq.timezone:Asia/Kuala_Lumpur}")
    public void collectTrafficSpeed() {
        if (!trafficSpeedRunning.compareAndSet(false, true)) {
            log.warn("CIQ上一轮TrafficSpeed采集尚未完成，本轮调度跳过，collector=CIQ_TRAFFIC_SPEED");
            return;
        }
        try {
            CiqTrafficSpeedCollector collector = collectorProvider.getIfAvailable();
            if (collector == null) {
                log.warn("CIQ调度已启用但TrafficSpeed采集器关闭，本轮不执行");
                return;
            }
            Instant scheduledTime = Instant.now();
            log.info("CIQ定时任务触发，collector=CIQ_TRAFFIC_SPEED，scheduledTime={}", scheduledTime);
            executor.execute(collector, CollectionContext.scheduled(
                    ModuleCode.CIQ, CollectorCode.CIQ_TRAFFIC_SPEED, scheduledTime));
        } finally {
            trafficSpeedRunning.set(false);
        }
    }
}

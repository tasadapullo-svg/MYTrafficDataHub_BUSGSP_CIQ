package com.mytransitgps.modules.ciq.storage;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CIQ 每日八接口目录初始化组件。
 *
 * <p>CIQ 模块启用后，在应用就绪和每天 00:00 创建当天 {@code CIQ/yyyyMMdd/API01...API08} 目录；
 * 不发送接口请求，也不执行数据库写入。
 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq", name = "enabled", havingValue = "true")
public class CiqDirectoryInitializer {
    private static final Logger log = LoggerFactory.getLogger(CiqDirectoryInitializer.class);

    private final CiqJsonStorageService storageService;
    private final CiqProperties properties;

    public CiqDirectoryInitializer(CiqJsonStorageService storageService, CiqProperties properties) {
        this.storageService = storageService;
        this.properties = properties;
    }

    /** 应用就绪后初始化当天 CIQ 八接口目录。 */
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        ensureToday("APPLICATION_READY");
    }

    /** 每天 00:00 初始化新日期目录，避免长时间运行跨日后目录缺失。 */
    @Scheduled(cron = "0 0 0 * * *", zone = "${traffic.ciq.timezone:Asia/Kuala_Lumpur}")
    public void initializeNewDay() {
        ensureToday("MIDNIGHT");
    }

    private void ensureToday(String trigger) {
        LocalDate today = LocalDate.now(ZoneId.of(properties.getTimezone()));
        var root = storageService.ensureDailyLayout(today);
        log.info("CIQ每日八接口目录初始化完成，trigger={}，date={}，apiFolderCount={}，path={}",
                trigger, today, CiqApiCode.values().length, root);
    }
}

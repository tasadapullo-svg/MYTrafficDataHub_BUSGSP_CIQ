package com.mytransitgps.gtfs.service;

import com.mytransitgps.gtfs.config.GtfsFeedRegistry;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BUS GPS 每日 JSON 目录初始化组件。
 *
 * <p>应用就绪和每日零点创建 {@code data_download/BusGPS/yyyyMMdd/<city>/}，
 * 只负责文件目录，不触发任何 GTFS 请求，也不修改数据库业务。</p>
 */
@Component
@ConditionalOnProperty(prefix = "traffic.bus-gps", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BusGpsDirectoryInitializer {

    private static final Logger log = LoggerFactory.getLogger(BusGpsDirectoryInitializer.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private final WorkspaceRootResolver workspaceRootResolver;
    private final GtfsFeedRegistry feedRegistry;

    public BusGpsDirectoryInitializer(WorkspaceRootResolver workspaceRootResolver, GtfsFeedRegistry feedRegistry) {
        this.workspaceRootResolver = workspaceRootResolver;
        this.feedRegistry = feedRegistry;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        ensureToday("APPLICATION_READY");
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kuala_Lumpur")
    public void initializeNewDay() {
        ensureToday("MIDNIGHT");
    }

    private void ensureToday(String trigger) {
        String date = DATE.format(LocalDate.now(ZONE));
        Path dailyRoot = BusGpsStorageLayout.dailyJsonRoot(workspaceRootResolver.resolve(), date);
        try {
            Files.createDirectories(dailyRoot);
            feedRegistry.getEnabledFeeds().stream()
                    .map(feed -> feed.cityFolder())
                    .distinct()
                    .forEach(city -> createDirectory(dailyRoot.resolve(city)));
            log.info("BUS GPS每日JSON目录初始化完成，trigger={}，date={}，cityFolderCount={}，path={}",
                    trigger, date,
                    feedRegistry.getEnabledFeeds().stream().map(feed -> feed.cityFolder()).distinct().count(),
                    dailyRoot);
        } catch (IOException ex) {
            log.error("BUS GPS每日JSON目录初始化失败，date={}，path={}，错误信息={}",
                    date, dailyRoot, ex.getMessage(), ex);
            throw new IllegalStateException("BUS GPS每日JSON目录初始化失败: " + dailyRoot, ex);
        }
    }

    private void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException ex) {
            throw new IllegalStateException("BUS GPS城市目录创建失败: " + path, ex);
        }
    }
}

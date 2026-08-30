package com.mytransitgps.config;

import com.mytransitgps.gtfs.archive.DailyArchiveProperties;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.config.BusGpsProperties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用生命周期日志监听器，集中记录 Context Ready、非敏感配置摘要、待命状态和安全关闭。
 */
@Component
public class ApplicationStartupLoggingListener implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ApplicationStartupLoggingListener.class);
    private final Environment environment;
    private final MyTransitGpsDatabaseProperties databaseProperties;
    private final DailyArchiveProperties archiveProperties;
    private final AppRedisProperties redisProperties;
    private final BusGpsProperties busGpsProperties;
    private final CiqProperties ciqProperties;
    private final AtomicBoolean running = new AtomicBoolean();

    public ApplicationStartupLoggingListener(Environment environment,
                                             MyTransitGpsDatabaseProperties databaseProperties,
                                             DailyArchiveProperties archiveProperties,
                                             AppRedisProperties redisProperties,
                                             BusGpsProperties busGpsProperties,
                                             CiqProperties ciqProperties) {
        this.environment = environment;
        this.databaseProperties = databaseProperties;
        this.archiveProperties = archiveProperties;
        this.redisProperties = redisProperties;
        this.busGpsProperties = busGpsProperties;
        this.ciqProperties = ciqProperties;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        String profiles = environment.getActiveProfiles().length == 0
                ? "default" : String.join(",", environment.getActiveProfiles());
        String workspaceRoot = databaseProperties.getWorkspaceRoot();
        if (workspaceRoot == null || workspaceRoot.isBlank()) {
            workspaceRoot = "AUTO_DETECT";
        }
        log.info("Spring Context初始化完成，activeProfiles={}", profiles);
        log.info("MYTrafficDataHub启动配置：profiles={}，serverPort={}，databaseEnabled={}，dbTestEnabled={}，multicityTestEnabled={}，continuousEnabled={}，dailyArchiveEnabled={}，redisStartupCheckEnabled={}，workspaceRoot={}",
                profiles,
                environment.getProperty("server.port", "8080"),
                databaseProperties.database.isEnabled(),
                databaseProperties.dbTest.isEnabled(),
                databaseProperties.multicityDbTest.isEnabled(),
                databaseProperties.continuous.isEnabled(),
                archiveProperties.isEnabled(),
                redisProperties.connectOnStartup(),
                workspaceRoot);
        log.info("交通模块状态：busGpsEnabled={}，busGpsFeedCount={}，ciqEnabled={}，ciqScheduleEnabled={}，ciqArchiveEnabled={}，ciqDatabaseWriteEnabled={}，ltaAccountKeyConfigured={}",
                busGpsProperties.isEnabled(), busGpsProperties.getFeeds().size(),
                ciqProperties.isEnabled(), ciqProperties.getSchedule().isEnabled(),
                ciqProperties.getArchive().isEnabled(),
                ciqProperties.getPersistence().isDatabaseWriteEnabled(),
                ciqProperties.isAccountKeyConfigured());
        log.info("CIQ 8接口启用状态：API01={}，API02={}，API03={}，API04={}，API05={}，API06={}，API07={}，API08={}",
                ciqProperties.getCollectors().getTrafficSpeed().isEnabled(),
                ciqProperties.getCollectors().getEstimatedTravelTimes().isEnabled(),
                ciqProperties.getCollectors().getTrafficIncidents().isEnabled(),
                ciqProperties.getCollectors().getVms().isEnabled(),
                ciqProperties.getCollectors().getFaultyTrafficLights().isEnabled(),
                ciqProperties.getCollectors().getRoadWorks().isEnabled(),
                ciqProperties.getCollectors().getTrafficFlow().isEnabled(),
                ciqProperties.getCollectors().getRoadOpenings().isEnabled());
        if (!databaseProperties.continuous.isEnabled()) {
            log.info("当前未启用自动多城市连续采集，服务处于待命状态");
        }
        log.info("MYTrafficDataHub服务已就绪，稳定模块=MYTransitGPS");
    }

    @EventListener
    public void onContextClosed(ContextClosedEvent event) {
        log.info("MYTrafficDataHub正在关闭");
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        running.set(false);
        log.info("MYTrafficDataHub已完成关闭流程");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        // 最低阶段保证该组件在其他业务 Lifecycle 之后停止，从而记录最终关闭完成。
        return Integer.MIN_VALUE;
    }
}

package com.mytransitgps.persistence.service;

import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 五路 BUS GPS 无固定结束时间的连续采集生命周期组件。
 *
 * <p>在 Spring Boot 就绪后启动现有多城市采集 Runner，保持每 Feed 120 秒采集语义和失败后延迟重启策略。
 * 本类是 BUS GPS 长期运行核心边界，CIQ 扩展不得改变其采集算法、线程语义或数据库结果。
 */
@Component
@ConditionalOnProperty(prefix = "mytransitgps.continuous", name = "enabled", havingValue = "true")
public class MulticityContinuousCollectionLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MulticityContinuousCollectionLifecycle.class);
    private final MulticityDatabaseTenMinuteRunner runner;
    private final WorkspaceRootResolver workspaceRootResolver;
    private final MyTransitGpsDatabaseProperties properties;
    private final Environment environment;
    private final AtomicBoolean running = new AtomicBoolean();
    private ExecutorService executor;

    public MulticityContinuousCollectionLifecycle(MulticityDatabaseTenMinuteRunner runner,
                                                  WorkspaceRootResolver workspaceRootResolver,
                                                  MyTransitGpsDatabaseProperties properties,
                                                  Environment environment) {
        this.runner = runner;
        this.workspaceRootResolver = workspaceRootResolver;
        this.properties = properties;
        this.environment = environment;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        start();
    }

    @Override
    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        String configuredPassword = environment.getProperty("spring.datasource.password");
        if (configuredPassword == null || configuredPassword.isBlank()) {
            running.set(false);
            log.error("DATABASE_PASSWORD_MISSING：spring.datasource.password为空，正式Continuous采集不会启动");
            return;
        }
        log.info("连续多城市长期采集准备启动，feedCount={}，intervalSeconds={}，durationMode=UNBOUNDED",
                properties.multicityDbTest.getFeeds().size(), properties.multicityDbTest.getIntervalSeconds());
        executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "mytransitgps-continuous-collector");
            thread.setDaemon(false);
            return thread;
        });
        executor.submit(this::runLoop);
        log.info("连续多城市采集生命周期已启动；每Feed间隔={}秒，无固定结束时长，应用运行期间持续采集",
                properties.multicityDbTest.getIntervalSeconds());
    }

    private void runLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                log.info("开始无固定时长多城市连续采集Run；停止条件=应用Stop/线程中断/不可恢复故障");
                runner.runContinuous(workspaceRootResolver.resolve());
                if (running.get() && !Thread.currentThread().isInterrupted()) {
                    log.warn("无固定时长连续采集Run在未收到Stop时提前返回，将在配置的延迟后重新启动");
                    waitFor(Duration.ofSeconds(Math.max(5, properties.continuous.getRetryDelaySeconds())));
                }
            } catch (RuntimeException ex) {
                if (!running.get() || Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.error("无固定时长连续多城市采集Run失败，将在配置的延迟后重新启动，错误信息={}", ex.getMessage(), ex);
                waitFor(Duration.ofSeconds(Math.max(5, properties.continuous.getRetryDelaySeconds())));
            }
        }
        log.info("连续多城市采集生命周期已退出");
    }

    private void waitFor(Duration duration) {
        try {
            Thread.sleep(Math.max(1L, duration.toMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void stop() {
        log.info("收到连续采集停止请求");
        running.set(false);
        if (executor == null) {
            log.info("连续采集线程已安全停止，collectorCreated=false");
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                log.warn("连续采集线程未在15秒内完全退出；当前Snapshot事务将由数据库保证原子性");
            } else {
                log.info("连续采集线程已安全停止，collectorCreated=true");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("等待连续采集线程停止时收到中断信号");
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}

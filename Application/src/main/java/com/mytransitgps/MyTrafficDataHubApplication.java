package com.mytransitgps;

import com.mytransitgps.gtfs.archive.DailyArchiveProperties;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.platform.config.BusGpsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * MYTrafficDataHub 正式 Spring Boot 启动入口。
 *
 * <p>该入口位于 Application 模块，只负责装配 Common、BusGPS 和 CIQ 模块，不承载 Parser、QC、
 * Repository 或 Collector 业务实现。</p>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        DailyArchiveProperties.class,
        MyTransitGpsDatabaseProperties.class,
        BusGpsProperties.class,
        CiqProperties.class
})
public class MyTrafficDataHubApplication {
    private static final Logger log = LoggerFactory.getLogger(MyTrafficDataHubApplication.class);

    public static void main(String[] args) {
        log.info("MYTrafficDataHub 正在启动，javaVersion={}", System.getProperty("java.version"));
        SpringApplication.run(MyTrafficDataHubApplication.class, args);
    }
}

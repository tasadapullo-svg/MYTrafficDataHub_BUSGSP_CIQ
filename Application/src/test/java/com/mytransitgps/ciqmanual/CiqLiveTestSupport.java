package com.mytransitgps.ciqmanual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.MyTrafficDataHubApplication;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionExecutor;
import com.mytransitgps.platform.collection.CollectionResult;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.DataCollector;
import com.mytransitgps.platform.collection.ModuleCode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * CIQ 单接口真实联网 + PostgreSQL 集成测试基类。
 *
 * <p>默认跳过，只有显式设置环境变量 {@code CIQ_LIVE_TEST=true} 后才会真实请求
 * LTA DataMall、保存 Raw 文件并写入本机 PostgreSQL。</p>
 *
 * <p>每个 API 测试除校验 collection_run / collection_artifact 外，还必须通过
 * API 专属 SQL 验证业务表确实存在本次 runUid 的写入结果。</p>
 */
@SpringBootTest(
        classes = MyTrafficDataHubApplication.class,
        properties = {
                "spring.profiles.active=longrun",
                "traffic.ciq.schedule.enabled=false"
        })
abstract class CiqLiveTestSupport {

    @Autowired
    CollectionExecutor executor;

    @Autowired
    JdbcTemplate jdbc;

    CollectionResult execute(DataCollector collector,
                             CollectorCode collectorCode,
                             String apiDataCountSql) {
        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(System.getenv("CIQ_LIVE_TEST")),
                "设置 CIQ_LIVE_TEST=true 后执行真实联网 + PostgreSQL 测试");

        CollectionContext context = new CollectionContext(
                UUID.randomUUID(),
                Instant.now(),
                Instant.now(),
                ModuleCode.CIQ,
                collectorCode,
                UUID.randomUUID().toString(),
                true,
                Map.of("test", "manual-live"));

        CollectionResult result = executor.execute(collector, context);
        assertTrue(result.success(),
                () -> "采集失败: " + result.errorCode() + " " + result.errorMessage());

        Integer runs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lta.collection_run WHERE uid=?",
                Integer.class,
                result.runUid());
        assertEquals(1, runs);

        Integer artifacts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lta.collection_artifact WHERE run_uid=?",
                Integer.class,
                result.runUid());
        assertTrue(artifacts != null && artifacts > 0,
                "必须生成 Raw JSON / DATA_FILE artifact");

        Integer businessRows = jdbc.queryForObject(
                apiDataCountSql,
                Integer.class,
                result.runUid());
        assertTrue(businessRows != null && businessRows > 0,
                () -> "业务表必须存在本次 runUid 的数据，collector=" + collectorCode
                        + ", runUid=" + result.runUid());

        return result;
    }
}

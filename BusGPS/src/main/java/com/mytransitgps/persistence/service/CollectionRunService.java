package com.mytransitgps.persistence.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.mytransitgps.persistence.entity.CoreCollectionRunEntity;
import com.mytransitgps.persistence.mapper.CoreCollectionRunMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 中文名称：数据库采集运行记录服务。
 *
 * 功能说明：创建并最终更新 core.collection_run，保留计划轮次请求统计和绝对调度漂移指标。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class CollectionRunService {

    private static final Logger log = LoggerFactory.getLogger(CollectionRunService.class);
    private final CoreCollectionRunMapper mapper;
    private final JdbcTemplate jdbc;

    public CollectionRunService(CoreCollectionRunMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    @Transactional
    public CoreCollectionRunEntity start(String runCode, Instant plannedStart, int cycles) {
        return start(runCode, "DB_TEN_MINUTE", plannedStart, cycles);
    }

    @Transactional
    public CoreCollectionRunEntity start(String runCode, String runType, Instant plannedStart, int cycles) {
        CoreCollectionRunEntity row = new CoreCollectionRunEntity();
        row.uid = UUID.randomUUID(); row.runCode = runCode; row.runType = runType; row.runStatus = "RUNNING";
        row.timezoneName = "Asia/Kuala_Lumpur"; row.plannedStartTime = plannedStart; row.actualStartTime = Instant.now();
        row.plannedCycleCount = cycles; row.actualCycleCount = 0; row.feedCount = 1; row.plannedRequestCount = cycles;
        row.actualRequestCount = 0; row.successfulRequestCount = 0; row.failedRequestCount = 0;
        row.http429Count = 0; row.http5xxCount = 0; row.timeoutCount = 0;
        row.remarks = "Johor Bahru disk JSON to PostgreSQL explicit validation run";
        mapper.insert(row);
        log.info("开始数据库采集测试，runType={}，runCode={}，runUid={}，计划轮次={}", runType, runCode, row.uid, cycles);
        return row;
    }

    @Transactional
    public CoreCollectionRunEntity startMulticity(String runCode, Instant plannedStart, int feeds, int cyclesPerFeed) {
        return startMulticity(runCode, "MULTICITY_DB_TEN_MINUTE", plannedStart, feeds, cyclesPerFeed,
                "Five-feed disk JSON to PostgreSQL explicit validation run");
    }

    @Transactional
    public CoreCollectionRunEntity startMulticity(String runCode, String runType, Instant plannedStart,
                                                   int feeds, int cyclesPerFeed, String remarks) {
        // 计划请求数由 Feed 数 × 每 Feed 周期数计算，作为窗口完整性的硬基线。
        CoreCollectionRunEntity row = new CoreCollectionRunEntity();
        row.uid = UUID.randomUUID(); row.runCode = runCode; row.runType = runType; row.runStatus = "RUNNING";
        row.timezoneName = "Asia/Kuala_Lumpur"; row.plannedStartTime = plannedStart; row.actualStartTime = Instant.now();
        row.plannedCycleCount = cyclesPerFeed; row.actualCycleCount = 0; row.feedCount = feeds;
        row.plannedRequestCount = feeds * cyclesPerFeed; row.actualRequestCount = 0; row.successfulRequestCount = 0;
        row.failedRequestCount = 0; row.http429Count = 0; row.http5xxCount = 0; row.timeoutCount = 0;
        row.remarks = remarks;
        mapper.insert(row);
        log.info("开始五Feed数据库采集测试，runCode={}，runUid={}，feed数={}，每Feed轮次={}，计划请求={}",
                runCode, row.uid, feeds, cyclesPerFeed, feeds * cyclesPerFeed);
        return row;
    }

    /**
     * 创建无固定时长的五Feed连续采集Run。planned_cycle_count/planned_request_count保持NULL，
     * 表示该Run没有预设结束轮次，仅在应用正常停止或发生不可恢复故障时结束。
     */
    @Transactional
    public CoreCollectionRunEntity startContinuousMulticity(String runCode, Instant plannedStart, int feeds) {
        CoreCollectionRunEntity row = new CoreCollectionRunEntity();
        row.uid = UUID.randomUUID(); row.runCode = runCode; row.runType = "MULTICITY_CONTINUOUS"; row.runStatus = "RUNNING";
        row.timezoneName = "Asia/Kuala_Lumpur"; row.plannedStartTime = plannedStart; row.actualStartTime = Instant.now();
        row.plannedCycleCount = null; row.actualCycleCount = 0; row.feedCount = feeds;
        row.plannedRequestCount = null; row.actualRequestCount = 0; row.successfulRequestCount = 0;
        row.failedRequestCount = 0; row.http429Count = 0; row.http5xxCount = 0; row.timeoutCount = 0;
        row.remarks = "Five-feed indefinite continuous collection; no fixed duration; stops with application lifecycle";
        mapper.insert(row);
        log.info("开始五Feed无固定时长连续采集，runCode={}，runUid={}，feed数={}，plannedCycles=UNBOUNDED，plannedRequests=UNBOUNDED",
                runCode, row.uid, feeds);
        return row;
    }

    /** 长期采集每完成一轮五Feed后更新一次进度，避免异常退出时Run统计长期停留在0。 */
    @Transactional
    public void updateContinuousMulticityProgress(CoreCollectionRunEntity row, int completedCycles, int actualRequests,
                                                   int successful, int failed, int http429, int http5xx, int timeout,
                                                   Double driftMaxMs) {
        row.actualCycleCount = completedCycles; row.actualRequestCount = actualRequests;
        row.successfulRequestCount = successful; row.failedRequestCount = failed;
        row.http429Count = http429; row.http5xxCount = http5xx; row.timeoutCount = timeout;
        if (driftMaxMs != null && (row.schedulerDriftMaxMs == null || driftMaxMs > row.schedulerDriftMaxMs)) {
            row.schedulerDriftMaxMs = driftMaxMs;
        }
        mapper.updateById(row);
    }

    /** 应用正常停止时对无固定时长Run进行正常收尾，而不是将人工Stop误记为FAILED。 */
    @Transactional
    public void finishContinuousMulticity(CoreCollectionRunEntity row, int completedCycles, int actualRequests,
                                           int successful, int failed, int http429, int http5xx, int timeout,
                                           Double driftMaxMs) {
        updateContinuousMulticityProgress(row, completedCycles, actualRequests, successful, failed,
                http429, http5xx, timeout, driftMaxMs);
        row.actualEndTime = Instant.now();
        row.runStatus = failed > 0 ? "SUCCESS_WITH_WARNINGS" : "SUCCESS";
        row.remarks = "Continuous collection stopped normally by application lifecycle; no fixed duration; evidence retained";
        mapper.updateById(row);
        log.info("五Feed无固定时长连续采集已正常收尾，runUid={}，status={}，完整轮次={}，请求={}，成功={}，失败={}",
                row.uid, row.runStatus, completedCycles, actualRequests, successful, failed);
    }

    @Transactional
    public void finishMulticity(CoreCollectionRunEntity row, int completedCyclesPerFeed, int actualRequests,
                                int successful, int failed, int http429, int http5xx, int timeout,
                                List<Long> drifts, boolean warnings, boolean coreFailure) {
        // 实际数量、漂移统计和最终状态在同一次事务更新中落库，防止状态与证据脱节。
        row.actualEndTime = Instant.now(); row.actualCycleCount = completedCyclesPerFeed; row.actualRequestCount = actualRequests;
        row.successfulRequestCount = successful; row.failedRequestCount = failed; row.http429Count = http429;
        row.http5xxCount = http5xx; row.timeoutCount = timeout;
        List<Long> sorted = new ArrayList<>(drifts); sorted.sort(Comparator.naturalOrder());
        row.schedulerDriftP50Ms = percentile(sorted, 0.50d); row.schedulerDriftP95Ms = percentile(sorted, 0.95d);
        row.schedulerDriftMaxMs = sorted.isEmpty() ? null : sorted.get(sorted.size() - 1).doubleValue();
        row.runStatus = coreFailure ? "FAILED" : (warnings ? "SUCCESS_WITH_WARNINGS" : "SUCCESS");
        mapper.updateById(row);
        log.info("五Feed计划采集完成，runUid={}，status={}，请求={}，成功={}，失败={}",
                row.uid, row.runStatus, actualRequests, successful, failed);
    }

    @Transactional
    public void finish(CoreCollectionRunEntity row, int actualCycles, int successful, int failed,
                       int http429, int http5xx, int timeout, List<Long> drifts, boolean warnings, boolean coreFailure) {
        row.actualEndTime = Instant.now(); row.actualCycleCount = actualCycles; row.actualRequestCount = actualCycles;
        row.successfulRequestCount = successful; row.failedRequestCount = failed; row.http429Count = http429;
        row.http5xxCount = http5xx; row.timeoutCount = timeout;
        List<Long> sorted = new ArrayList<>(drifts); sorted.sort(Comparator.naturalOrder());
        row.schedulerDriftP50Ms = percentile(sorted, 0.50d); row.schedulerDriftP95Ms = percentile(sorted, 0.95d);
        row.schedulerDriftMaxMs = sorted.isEmpty() ? null : sorted.get(sorted.size() - 1).doubleValue();
        row.runStatus = coreFailure ? "FAILED" : (warnings ? "SUCCESS_WITH_WARNINGS" : "SUCCESS");
        mapper.updateById(row);
        log.info("{}轮Realtime采集完成，runUid={}，status={}，成功={}，失败={}", actualCycles, row.uid, row.runStatus, successful, failed);
    }

    @Transactional
    public void markValidationFailed(CoreCollectionRunEntity row, String reason) {
        row.runStatus = "FAILED"; row.remarks = reason; row.actualEndTime = Instant.now(); mapper.updateById(row);
        log.error("Run数据库验收失败，runUid={}，原因={}", row.uid, reason);
    }

    @Transactional
    public void markValidationPassed(CoreCollectionRunEntity row, boolean warnings) {
        row.runStatus = warnings ? "SUCCESS_WITH_WARNINGS" : "SUCCESS";
        row.remarks = "Disk JSON to PostgreSQL validation PASS"; mapper.updateById(row);
        log.info("Run数据库验收通过，runUid={}，status={}", row.uid, row.runStatus);
    }

    @Transactional
    public void markCollectionPassed(CoreCollectionRunEntity row, boolean warnings, String remarks) {
        row.runStatus = warnings ? "SUCCESS_WITH_WARNINGS" : "SUCCESS";
        row.remarks = remarks;
        mapper.updateById(row);
        log.info("Run连续采集窗口完成，runUid={}，status={}，remarks={}", row.uid, row.runStatus, remarks);
    }

    @Transactional
    public void markInterruptedMulticity(CoreCollectionRunEntity row, String reason) {
        // 中断时以四个城市 Schema 的请求日志重算统计，不依赖尚未返回的内存列表。
        String sql = "SELECT result,http_status,error_class,scheduler_drift_ms,response_received_at FROM ("
                + "SELECT result,http_status,error_class,scheduler_drift_ms,response_received_at FROM jb.api_request_log WHERE run_uid=? "
                + "UNION ALL SELECT result,http_status,error_class,scheduler_drift_ms,response_received_at FROM kuching.api_request_log WHERE run_uid=? "
                + "UNION ALL SELECT result,http_status,error_class,scheduler_drift_ms,response_received_at FROM kl.api_request_log WHERE run_uid=? "
                + "UNION ALL SELECT result,http_status,error_class,scheduler_drift_ms,response_received_at FROM melaka.api_request_log WHERE run_uid=?"
                + ") requests";
        var requests = jdbc.queryForList(sql, row.uid, row.uid, row.uid, row.uid);
        int successful = 0, http429 = 0, http5xx = 0, timeouts = 0;
        Instant endedAt = row.actualStartTime;
        List<Long> drifts = new ArrayList<>();
        for (var request : requests) {
            if ("SUCCESS".equals(request.get("result"))) successful++;
            Number status = (Number) request.get("http_status");
            if (status != null && status.intValue() == 429) http429++;
            if (status != null && status.intValue() >= 500 && status.intValue() <= 599) http5xx++;
            if ("HttpTimeoutException".equals(request.get("error_class"))) timeouts++;
            Number drift = (Number) request.get("scheduler_drift_ms");
            if (drift != null) drifts.add(drift.longValue());
            Instant responseTime = instant(request.get("response_received_at"));
            if (responseTime != null && (endedAt == null || responseTime.isAfter(endedAt))) endedAt = responseTime;
        }
        drifts.sort(Comparator.naturalOrder());
        row.actualEndTime = endedAt == null ? Instant.now() : endedAt;
        row.actualRequestCount = requests.size();
        row.successfulRequestCount = successful;
        row.failedRequestCount = requests.size() - successful;
        row.actualCycleCount = row.feedCount == null || row.feedCount == 0 ? 0 : requests.size() / row.feedCount;
        row.http429Count = http429;
        row.http5xxCount = http5xx;
        row.timeoutCount = timeouts;
        row.schedulerDriftP50Ms = percentile(drifts, 0.50d);
        row.schedulerDriftP95Ms = percentile(drifts, 0.95d);
        row.schedulerDriftMaxMs = drifts.isEmpty() ? null : drifts.get(drifts.size() - 1).doubleValue();
        row.runStatus = "FAILED";
        String plannedRequests = row.plannedRequestCount == null ? "UNBOUNDED" : row.plannedRequestCount.toString();
        row.remarks = "INTERRUPTED; partial evidence retained; " + requests.size() + "/" + plannedRequests
                + " requests; " + reason;
        mapper.updateById(row);
        log.warn("多城市Run已按请求证据安全收尾，runUid={}，请求={}/{}，完整周期={}，原因={}",
                row.uid, requests.size(), plannedRequests, row.actualCycleCount, reason);
    }

    private Instant instant(Object value) {
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof OffsetDateTime offset) return offset.toInstant();
        return value == null ? null : Instant.parse(value.toString());
    }

    private Double percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return null;
        int index = (int) Math.ceil(percentile * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1))).doubleValue();
    }
}

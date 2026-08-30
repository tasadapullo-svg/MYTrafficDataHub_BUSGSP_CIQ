package com.mytransitgps.persistence.service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.mytransitgps.gtfs.client.GtfsHttpResult;
import com.mytransitgps.persistence.entity.JbApiRequestLogEntity;
import com.mytransitgps.persistence.mapper.JbApiRequestLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 中文名称：GTFS API 请求证据独立事务服务。
 *
 * 功能说明：每个真实 HTTP attempt 立即写入 jb.api_request_log；后续 JSON 或 Snapshot
 * 事务失败时仍保留请求事实，并以独立事务更新失败状态。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class ApiRequestEvidenceService {

    private static final Logger log = LoggerFactory.getLogger(ApiRequestEvidenceService.class);
    private final JbApiRequestLogMapper mapper;

    public ApiRequestEvidenceService(JbApiRequestLogMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JbApiRequestLogEntity create(UUID feedUid, UUID runUid, UUID duplicateOfRequestUid,
                                         int requestSequence, int cycle, Instant scheduledAt,
                                         GtfsHttpResult http) {
        // 请求证据使用独立事务先落库，即使后续解析或主数据事务失败也能保留失败原因。
        JbApiRequestLogEntity row = new JbApiRequestLogEntity();
        row.uid = UUID.randomUUID(); row.feedUid = feedUid; row.runUid = runUid; row.duplicateOfRequestUid = duplicateOfRequestUid;
        row.requestType = "REALTIME"; row.requestSequence = requestSequence; row.cycleNumber = cycle;
        row.requestedUrl = http.requestedUri().toString(); row.finalUrl = http.finalUri().toString(); row.scheduledAt = scheduledAt;
        row.requestStartedAt = http.requestStartedAt(); row.responseReceivedAt = http.responseReceivedAt(); row.latencyMs = http.latencyMs();
        row.schedulerDriftMs = Duration.between(scheduledAt, http.requestStartedAt()).toMillis(); row.httpStatus = http.statusCode();
        row.contentType = http.contentType(); row.responseBytes = (long) http.responseBytes(); row.responseSha256 = http.responseSha256(); row.redirectCount = http.redirectCount();
        row.parseStatus = "PENDING"; row.duplicateSnapshot = duplicateOfRequestUid != null;
        row.result = http.isHttpOk() ? "HTTP_SUCCESS" : "HTTP_FAILED"; row.errorClass = http.errorClass(); row.errorMessage = http.errorMessage();
        mapper.insert(row);
        log.info("HTTP请求事实已写入数据库，cycle={}，requestUid={}，httpStatus={}，bytes={}", cycle, row.uid, row.httpStatus, row.responseBytes);
        return row;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFilesAndParse(JbApiRequestLogEntity row, String parseStatus, int entityCount, int vehicleCount,
                                  Path workspaceRoot, Path rawPath, Path parsedPath, Path enrichedPath,
                                  boolean duplicateSnapshot, String result, Throwable error) {
        // 文件路径只保存工作区相对路径，方便工作区整体迁移及归档后复核。
        row.parseStatus = parseStatus; row.entityCount = entityCount; row.vehicleCount = vehicleCount; row.duplicateSnapshot = duplicateSnapshot;
        row.rawObjectPath = relative(workspaceRoot, rawPath); row.parsedJsonPath = relative(workspaceRoot, parsedPath); row.enrichedJsonPath = relative(workspaceRoot, enrichedPath);
        row.result = result; row.errorClass = error == null ? null : error.getClass().getSimpleName(); row.errorMessage = error == null ? null : error.getMessage();
        mapper.updateById(row);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDatabaseFailure(JbApiRequestLogEntity row, Throwable error) {
        row.result = "DB_PERSISTENCE_FAILED"; row.errorClass = error.getClass().getSimpleName(); row.errorMessage = error.getMessage();
        mapper.updateById(row);
        log.error("请求对应Snapshot数据库持久化失败，requestUid={}，错误信息={}", row.uid, error.getMessage(), error);
    }

    private String relative(Path root, Path path) {
        return path == null ? null : root.relativize(path).toString().replace('\\', '/');
    }
}

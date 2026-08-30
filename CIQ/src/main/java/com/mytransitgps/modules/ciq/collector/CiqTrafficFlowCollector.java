package com.mytransitgps.modules.ciq.collector;

import com.mytransitgps.modules.ciq.client.LtaTrafficFlowClient;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.parser.TrafficFlowParser;
import com.mytransitgps.modules.ciq.persistence.CiqAuditPersistenceService;
import com.mytransitgps.modules.ciq.persistence.TrafficFlowPersistenceService;
import com.mytransitgps.modules.ciq.quality.TrafficFlowValidator;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionResult;
import com.mytransitgps.platform.collection.CollectionStatus;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.DataCollector;
import com.mytransitgps.platform.collection.ModuleCode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API07 Traffic Flow：获取临时 Link 后立即下载真实 Traffic Flow 文件并保存 Hash/元数据。
 *
 * <p>Raw 外层 JSON 与真实数据文件均优先落盘；数据库异常不回滚已保存的科研原始文件。</p>
 */
public class CiqTrafficFlowCollector implements DataCollector {
    private static final Logger log = LoggerFactory.getLogger(CiqTrafficFlowCollector.class);

    private final LtaTrafficFlowClient client;
    private final CiqProperties properties;
    private final CiqJsonStorageService storage;
    private final TrafficFlowParser parser;
    private final TrafficFlowValidator validator;
    private final TrafficFlowPersistenceService persistence;
    private final CiqAuditPersistenceService audit;

    public CiqTrafficFlowCollector(LtaTrafficFlowClient client,
                                   CiqProperties properties,
                                   CiqJsonStorageService storage,
                                   TrafficFlowParser parser,
                                   TrafficFlowValidator validator,
                                   TrafficFlowPersistenceService persistence,
                                   CiqAuditPersistenceService audit) {
        this.client = client;
        this.properties = properties;
        this.storage = storage;
        this.parser = parser;
        this.validator = validator;
        this.persistence = persistence;
        this.audit = audit;
    }

    @Override
    public ModuleCode moduleCode() {
        return ModuleCode.CIQ;
    }

    @Override
    public CollectorCode collectorCode() {
        return CollectorCode.CIQ_TRAFFIC_FLOW;
    }

    @Override
    public CollectionResult collect(CollectionContext context) {
        Instant start = Instant.now();
        log.info("CIQ API07 [N01] Collector进入，runUid={}，scheduledTime={}，manual={}，dbWrite={}，endpoint={}",
                context.runUid(), context.scheduledTime(), context.manualTrigger(),
                properties.getPersistence().isDatabaseWriteEnabled(),
                properties.getCollectors().getTrafficFlow().getEndpoint());

        if (!properties.isAccountKeyConfigured()) {
            log.warn("CIQ API07 [N00] AccountKey未配置，本轮跳过，runUid={}", context.runUid());
            return CollectionResult.skipped(context, CollectionStatus.NOT_CONFIGURED, start,
                    "LTA_ACCOUNT_KEY_MISSING", "LTA AccountKey is not configured");
        }

        boolean dbRequested = properties.getPersistence().isDatabaseWriteEnabled();
        boolean dbActive = false;
        String dbErrorCode = null;
        String dbErrorMessage = null;

        if (dbRequested) {
            log.info("CIQ API07 [N02] 检查数据库审计与Persistence Bean，runUid={}", context.runUid());
            if (audit == null || persistence == null) {
                dbErrorCode = "PERSISTENCE_NOT_AVAILABLE";
                dbErrorMessage = "API07数据库审计或持久化Bean未装配";
                log.error("CIQ API07 [DB-N00] {}，但继续下载Raw文件，runUid={}", dbErrorMessage, context.runUid());
            } else {
                try {
                    audit.begin(context.runUid(), "API07",
                            context.manualTrigger() ? "MANUAL_TEST" : "SCHEDULED",
                            context.scheduledTime(), start);
                    dbActive = true;
                    log.info("CIQ API07 [DB-N01] collection_run初始化完成，runUid={}", context.runUid());
                } catch (RuntimeException ex) {
                    dbErrorCode = "DATABASE_AUDIT_INIT_FAILED";
                    dbErrorMessage = ex.getMessage();
                    log.error("CIQ API07 [DB-N00] collection_run初始化失败，但继续下载Raw文件，runUid={}，message={}",
                            context.runUid(), ex.getMessage(), ex);
                }
            }
        } else {
            log.info("CIQ API07 [N02] 数据库写入关闭，本轮执行Raw-only文件采集，runUid={}", context.runUid());
        }

        int requests = 0;
        long bytes = 0;
        Integer httpStatus = null;
        short retries = 0;
        boolean outerJsonSaved = false;
        boolean dataFileSaved = false;
        int inserted = 0;

        try {
            log.info("CIQ API07 [N03] 请求TrafficFlow外层JSON/临时Link，runUid={}", context.runUid());
            var response = client.fetchPage(client.firstPageUri());
            requests++;
            retries += response.retryCount();
            var http = response.http();
            httpStatus = http.statusCode();
            if (!http.successful()) {
                throw new IllegalStateException("API07 Link请求失败 HTTP=" + httpStatus);
            }
            log.info("CIQ API07 [N04] 外层JSON HTTP成功，runUid={}，status={}，bytes={}，latencyMs={}",
                    context.runUid(), httpStatus, http.body().length, http.latencyMs());

            LocalDate dataDate = http.requestEndTime()
                    .atZone(ZoneId.of(properties.getTimezone())).toLocalDate();
            var rawArtifact = storage.saveRawJson(CiqApiCode.API07, dataDate,
                    context.runUid(), 1, http.requestEndTime(), http.body());
            bytes += http.body().length;
            outerJsonSaved = true;
            log.info("CIQ API07 [N05] 外层Raw JSON落盘完成，runUid={}，path={}，bytes={}，sha256={}",
                    context.runUid(), rawArtifact.path(), rawArtifact.sizeBytes(), rawArtifact.sha256());

            if (dbActive) {
                try {
                    audit.artifact(context.runUid(), rawArtifact);
                    audit.page(context.runUid(), 1, 0, http.requestStartTime(), http.requestEndTime(),
                            httpStatus, 1, http.body().length, true, null);
                    log.info("CIQ API07 [DB-N02] 外层JSON artifact/page log登记完成，runUid={}", context.runUid());
                } catch (RuntimeException ex) {
                    dbErrorCode = "DATABASE_AUDIT_WRITE_FAILED";
                    dbErrorMessage = ex.getMessage();
                    log.error("CIQ API07 [DB-N99] 审计写入失败，但继续真实文件下载，runUid={}，message={}",
                            context.runUid(), ex.getMessage(), ex);
                    finishAuditSafely(context, httpStatus, requests, bytes, false, false, retries, ex.getMessage(), true);
                    dbActive = false;
                }
            }

            var parsedLinks = parser.parse(http.body());
            var validLinks = parsedLinks.stream().filter(validator::isValid).toList();
            log.info("CIQ API07 [N06] Parser/Validator完成，runUid={}，linkCount={}，validLinkCount={}",
                    context.runUid(), parsedLinks.size(), validLinks.size());
            if (validLinks.isEmpty()) {
                throw new IllegalStateException("API07响应未包含有效HTTPS Link");
            }

            URI link = URI.create(validLinks.get(0).link());
            log.info("CIQ API07 [N07] 临时下载Link解析完成，runUid={}，scheme={}，host={}",
                    context.runUid(), link.getScheme(), link.getHost());

            var fileResponse = client.downloadDataFile(link);
            requests++;
            httpStatus = fileResponse.statusCode();
            if (!fileResponse.successful()) {
                throw new IllegalStateException("API07原始文件下载失败 HTTP=" + httpStatus);
            }
            log.info("CIQ API07 [N08] 原始数据文件HTTP成功，runUid={}，status={}，bytes={}，latencyMs={}",
                    context.runUid(), httpStatus, fileResponse.body().length, fileResponse.latencyMs());

            String suggestedName = fileName(link);
            var dataArtifact = storage.saveDataFile(CiqApiCode.API07, dataDate, context.runUid(),
                    fileResponse.requestEndTime(), suggestedName, fileResponse.body());
            bytes += fileResponse.body().length;
            dataFileSaved = true;
            log.info("CIQ API07 [N09] 原始数据文件落盘完成，runUid={}，bytes={}，sha256={}，path={}",
                    context.runUid(), dataArtifact.sizeBytes(), dataArtifact.sha256(), dataArtifact.path());

            if (dbActive) {
                try {
                    audit.dataArtifact(context.runUid(), dataArtifact.path().getFileName().toString(),
                            dataArtifact.path().toString(), dataArtifact.sizeBytes(), dataArtifact.sha256());
                    inserted = persistence.persist(context.runUid(), dataDate.withDayOfMonth(1),
                            link.toString(), dataArtifact.path().getFileName().toString(),
                            dataArtifact.path().toString(), dataArtifact.sizeBytes(), dataArtifact.sha256(),
                            fileResponse.requestEndTime());
                    finishAuditSafely(context, httpStatus, requests, bytes, true, true, retries, null, true);
                    log.info("CIQ API07 [DB-N03] PostgreSQL文件元数据写入完成，runUid={}，affected={}",
                            context.runUid(), inserted);
                } catch (RuntimeException ex) {
                    dbErrorCode = "DATABASE_WRITE_FAILED";
                    dbErrorMessage = ex.getMessage();
                    log.error("CIQ API07 [DB-N99] 文件元数据写入失败，但Raw文件已保留，runUid={}，message={}",
                            context.runUid(), ex.getMessage(), ex);
                    finishAuditSafely(context, httpStatus, requests, bytes, false, false, retries, ex.getMessage(), true);
                    dbActive = false;
                }
            }

            Instant end = Instant.now();
            if (dbRequested && !dbActive) {
                log.warn("CIQ API07 [N10] Raw JSON + 原始数据文件完整，但数据库链未完成，runUid={}，dbErrorCode={}，dbErrorMessage={}",
                        context.runUid(), dbErrorCode, dbErrorMessage);
                return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode(), false,
                        CollectionStatus.PARTIAL_SUCCESS, start, end,
                        Duration.between(start, end).toMillis(), requests, 1, 1, 0,
                        inserted, 0, 0, 0, httpStatus,
                        dbErrorCode == null ? "DATABASE_WRITE_SKIPPED_RAW_SAVED" : dbErrorCode,
                        dbErrorMessage == null ? "API07 raw files saved; database chain unavailable" : dbErrorMessage);
            }

            log.info("CIQ API07 [N10] 完整采集结束，runUid={}，outerJsonSaved={}，dataFileSaved={}，requests={}，bytes={}，dbInserted={}，durationMs={}",
                    context.runUid(), outerJsonSaved, dataFileSaved, requests, bytes, inserted,
                    Duration.between(start, end).toMillis());
            return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode(), true,
                    CollectionStatus.SUCCESS, start, end, Duration.between(start, end).toMillis(),
                    requests, 1, 1, 0, inserted, 0, 0, 0, httpStatus, null, null);
        } catch (RuntimeException ex) {
            log.error("CIQ API07 [N99] Collector失败，runUid={}，outerJsonSaved={}，dataFileSaved={}，message={}",
                    context.runUid(), outerJsonSaved, dataFileSaved, ex.getMessage(), ex);
            finishAuditSafely(context, httpStatus, requests, bytes, false, false, retries,
                    ex.getMessage(), dbActive);
            Instant end = Instant.now();
            return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode(), false,
                    outerJsonSaved ? CollectionStatus.PARTIAL_SUCCESS : CollectionStatus.FAILED,
                    start, end, Duration.between(start, end).toMillis(), requests,
                    outerJsonSaved ? 1 : 0, outerJsonSaved ? 1 : 0,
                    0, inserted, 0, 0, 1, httpStatus,
                    ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private void finishAuditSafely(CollectionContext context,
                                   Integer httpStatus,
                                   int requests,
                                   long bytes,
                                   boolean successful,
                                   boolean complete,
                                   short retries,
                                   String error,
                                   boolean active) {
        if (!active || audit == null) {
            return;
        }
        try {
            audit.finish(context.runUid(), Instant.now(), httpStatus == null ? 0 : httpStatus,
                    requests, 1, bytes, successful, complete, retries, error);
        } catch (RuntimeException ex) {
            log.error("CIQ API07 [DB-N98] collection_run finalize失败，runUid={}，message={}",
                    context.runUid(), ex.getMessage(), ex);
        }
    }

    private String fileName(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || path.endsWith("/")) {
            return "traffic_flow_data.bin";
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }
}

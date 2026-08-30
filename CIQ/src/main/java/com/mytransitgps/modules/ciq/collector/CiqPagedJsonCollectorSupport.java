package com.mytransitgps.modules.ciq.collector;

import com.mytransitgps.modules.ciq.client.AbstractLtaJsonClient;
import com.mytransitgps.modules.ciq.client.LtaHttpResponse;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.persistence.CiqAuditPersistenceService;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.platform.collection.CollectionContext;
import com.mytransitgps.platform.collection.CollectionResult;
import com.mytransitgps.platform.collection.CollectionStatus;
import com.mytransitgps.platform.collection.CollectorCode;
import com.mytransitgps.platform.collection.ModuleCode;
import com.mytransitgps.platform.http.HttpRequestResult;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * API02/API03/API04/API05/API06/API08 的共用采集编排器。
 *
 * <p>每个 API 仍拥有独立 Client、Parser、Validator、Persistence、Collector 和 Scheduler。
 * 共用编排层只负责统一执行节点、分页、Raw 落盘、审计和异常降级。</p>
 *
 * <p>科研原始数据优先：数据库节点异常不会阻断后续 Raw 分页下载；如果 Raw 完整但数据库链失败，
 * 本轮返回 PARTIAL_SUCCESS，并在日志中保留明确的数据库错误节点。</p>
 */
final class CiqPagedJsonCollectorSupport<T> {

    interface Parser<T> {
        List<T> parse(byte[] body);
    }

    interface Persist<T> {
        int persist(UUID runUid, Instant snapshotTime, List<T> rows);
    }

    private final String api;
    private final CiqApiCode apiCode;
    private final CollectorCode collectorCode;
    private final AbstractLtaJsonClient client;
    private final CiqProperties properties;
    private final CiqProperties.CollectorSettings settings;
    private final CiqJsonStorageService storage;
    private final Parser<T> parser;
    private final Predicate<T> valid;
    private final Persist<T> persistence;
    private final CiqAuditPersistenceService audit;
    private final Logger log;

    CiqPagedJsonCollectorSupport(String api,
                                 CiqApiCode apiCode,
                                 CollectorCode collectorCode,
                                 AbstractLtaJsonClient client,
                                 CiqProperties properties,
                                 CiqProperties.CollectorSettings settings,
                                 CiqJsonStorageService storage,
                                 Parser<T> parser,
                                 Predicate<T> valid,
                                 Persist<T> persistence,
                                 CiqAuditPersistenceService audit,
                                 Logger log) {
        this.api = api;
        this.apiCode = apiCode;
        this.collectorCode = collectorCode;
        this.client = client;
        this.properties = properties;
        this.settings = settings;
        this.storage = storage;
        this.parser = parser;
        this.valid = valid;
        this.persistence = persistence;
        this.audit = audit;
        this.log = log;
    }

    CollectionResult collect(CollectionContext context) {
        Instant start = Instant.now();
        log.info("CIQ {} [N01] Collector进入，runUid={}，scheduledTime={}，manual={}，dbWrite={}，storageRoot={}，endpoint={}",
                api, context.runUid(), context.scheduledTime(), context.manualTrigger(),
                properties.getPersistence().isDatabaseWriteEnabled(),
                properties.getStorage().getRootDirectory(), settings.getEndpoint());

        if (!properties.isAccountKeyConfigured()) {
            log.warn("CIQ {} [N00] AccountKey未配置，本轮跳过，runUid={}", api, context.runUid());
            return CollectionResult.skipped(context, CollectionStatus.NOT_CONFIGURED, start,
                    "LTA_ACCOUNT_KEY_MISSING", "LTA AccountKey is not configured");
        }

        boolean dbRequested = properties.getPersistence().isDatabaseWriteEnabled();
        boolean dbActive = false;
        String dbErrorCode = null;
        String dbErrorMessage = null;

        if (dbRequested) {
            log.info("CIQ {} [N02] 检查数据库审计与Persistence Bean，runUid={}", api, context.runUid());
            if (audit == null || persistence == null) {
                dbErrorCode = "PERSISTENCE_NOT_AVAILABLE";
                dbErrorMessage = "数据库审计或持久化Bean未装配";
                log.error("CIQ {} [DB-N00] {}，但继续Raw采集，runUid={}", api, dbErrorMessage, context.runUid());
            } else {
                try {
                    audit.begin(context.runUid(), api,
                            context.manualTrigger() ? "MANUAL_TEST" : "SCHEDULED",
                            context.scheduledTime(), start);
                    dbActive = true;
                    log.info("CIQ {} [DB-N01] collection_run初始化完成，runUid={}", api, context.runUid());
                } catch (RuntimeException ex) {
                    dbErrorCode = "DATABASE_AUDIT_INIT_FAILED";
                    dbErrorMessage = ex.getMessage();
                    log.error("CIQ {} [DB-N00] collection_run初始化失败，但继续Raw采集，runUid={}，message={}",
                            api, context.runUid(), ex.getMessage(), ex);
                }
            }
        } else {
            log.info("CIQ {} [N02] 数据库写入关闭，本轮执行Raw-only采集，runUid={}", api, context.runUid());
        }

        int requestCount = 0;
        long received = 0;
        long validCount = 0;
        long rejected = 0;
        long rawBytes = 0;
        long inserted = 0;
        short retries = 0;
        Integer httpStatus = null;
        URI uri = client.firstPageUri();
        Set<String> visited = new HashSet<>();
        int pageSize = Math.max(1, settings.getPageSize());
        int maxPages = Math.max(1, settings.getMaxPages());
        Instant snapshotTime = start;

        try {
            for (int page = 1; page <= maxPages && uri != null; page++) {
                if (!visited.add(uri.toString())) {
                    throw new IllegalStateException("分页URI循环: " + uri);
                }

                long skip = (long) (page - 1) * pageSize;
                log.info("CIQ {} [N03] HTTP页面请求开始，runUid={}，page={}，skip={}，uri={}",
                        api, context.runUid(), page, skip, uri);

                LtaHttpResponse response = client.fetchPage(uri);
                HttpRequestResult http = response.http();
                requestCount++;
                retries += response.retryCount();
                httpStatus = http.statusCode();

                if (!http.successful()) {
                    log.error("CIQ {} [N03-ERR] HTTP最终失败，runUid={}，page={}，status={}，errorCode={}，message={}",
                            api, context.runUid(), page, http.statusCode(), http.errorCode(), http.errorMessage());
                    finishAuditSafely(context.runUid(), httpStatus, requestCount, received, rawBytes,
                            false, false, retries, http.errorMessage(), dbActive);
                    return failure(context, start, requestCount, received, validCount, rejected,
                            inserted, httpStatus,
                            http.errorCode() == null ? "HTTP_FAILED" : http.errorCode(),
                            http.errorMessage());
                }

                log.info("CIQ {} [N04] HTTP成功，runUid={}，page={}，status={}，bytes={}，latencyMs={}",
                        api, context.runUid(), page, httpStatus, http.body().length, http.latencyMs());

                LocalDate dataDate = http.requestEndTime()
                        .atZone(ZoneId.of(properties.getTimezone())).toLocalDate();
                var artifact = storage.saveRawJson(apiCode, dataDate, context.runUid(), page,
                        http.requestEndTime(), http.body());
                rawBytes += http.body().length;
                log.info("CIQ {} [N05] Raw JSON落盘完成，runUid={}，page={}，path={}，bytes={}，sha256={}",
                        api, context.runUid(), page, artifact.path(), artifact.sizeBytes(), artifact.sha256());

                List<T> parsed = parser.parse(http.body());
                received += parsed.size();
                List<T> accepted = parsed.stream().filter(valid).toList();
                long bad = parsed.size() - accepted.size();
                validCount += accepted.size();
                rejected += bad;
                log.info("CIQ {} [N06] Parser/Validator完成，runUid={}，page={}，parsed={}，valid={}，rejected={}",
                        api, context.runUid(), page, parsed.size(), accepted.size(), bad);

                if (dbActive) {
                    try {
                        audit.artifact(context.runUid(), artifact);
                        audit.page(context.runUid(), page, (int) skip,
                                http.requestStartTime(), http.requestEndTime(), httpStatus,
                                parsed.size(), http.body().length, true, null);
                        log.info("CIQ {} [DB-N02] Raw artifact/page log登记完成，runUid={}，page={}",
                                api, context.runUid(), page);

                        int affected = persistence.persist(context.runUid(), snapshotTime, accepted);
                        inserted += affected;
                        log.info("CIQ {} [N07] PostgreSQL持久化完成，runUid={}，page={}，accepted={}，affected={}，insertedTotal={}",
                                api, context.runUid(), page, accepted.size(), affected, inserted);
                    } catch (RuntimeException ex) {
                        dbErrorCode = "DATABASE_WRITE_FAILED";
                        dbErrorMessage = ex.getMessage();
                        log.error("CIQ {} [DB-N99] 数据库节点失败，本轮后续继续Raw采集，runUid={}，page={}，message={}",
                                api, context.runUid(), page, ex.getMessage(), ex);
                        finishAuditSafely(context.runUid(), httpStatus, requestCount, received, rawBytes,
                                false, false, retries, ex.getMessage(), true);
                        dbActive = false;
                    }
                }

                log.info("CIQ {} [N08] 页面闭环完成，runUid={}，page={}，records={}，receivedTotal={}，rawBytesTotal={}",
                        api, context.runUid(), page, parsed.size(), received, rawBytes);

                if (parsed.size() < pageSize) {
                    if (dbActive) {
                        finishAuditSafely(context.runUid(), httpStatus, requestCount, received, rawBytes,
                                true, true, retries, null, true);
                        Instant end = Instant.now();
                        log.info("CIQ {} [N09] Raw + PostgreSQL完整采集结束，runUid={}，pages={}，received={}，valid={}，rejected={}，inserted={}，durationMs={}",
                                api, context.runUid(), requestCount, received, validCount, rejected,
                                inserted, Duration.between(start, end).toMillis());
                        return success(context, start, requestCount, received, validCount,
                                rejected, inserted, httpStatus);
                    }

                    Instant end = Instant.now();
                    if (dbRequested) {
                        log.warn("CIQ {} [N09] Raw完整采集已完成，但数据库链未完成，runUid={}，pages={}，received={}，dbErrorCode={}，dbErrorMessage={}",
                                api, context.runUid(), requestCount, received, dbErrorCode, dbErrorMessage);
                        return partial(context, start, requestCount, received, validCount,
                                rejected, inserted, httpStatus,
                                dbErrorCode == null ? "DATABASE_WRITE_SKIPPED_RAW_SAVED" : dbErrorCode,
                                dbErrorMessage == null ? "Raw snapshot complete; database chain unavailable" : dbErrorMessage);
                    }

                    log.info("CIQ {} [N09] Raw-only完整采集结束，runUid={}，pages={}，received={}，valid={}，rejected={}，durationMs={}",
                            api, context.runUid(), requestCount, received, validCount, rejected,
                            Duration.between(start, end).toMillis());
                    return success(context, start, requestCount, received, validCount,
                            rejected, 0, httpStatus);
                }

                long nextSkip = (long) page * pageSize;
                URI base = client.firstPageUri();
                uri = URI.create(base.toString() + (base.toString().contains("?") ? "&" : "?")
                        + "$skip=" + nextSkip);
            }

            throw new IllegalStateException("达到maxPages仍未结束分页");
        } catch (RuntimeException ex) {
            log.error("CIQ {} [N99] Collector失败，runUid={}，message={}",
                    api, context.runUid(), ex.getMessage(), ex);
            finishAuditSafely(context.runUid(), httpStatus, requestCount, received, rawBytes,
                    false, false, retries, ex.getMessage(), dbActive);
            return failure(context, start, requestCount, received, validCount,
                    rejected + 1, inserted, httpStatus,
                    ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private void finishAuditSafely(UUID runUid,
                                   Integer httpStatus,
                                   int requestCount,
                                   long records,
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
            audit.finish(runUid, Instant.now(), httpStatus == null ? 0 : httpStatus,
                    requestCount, records, bytes, successful, complete, retries, error);
        } catch (RuntimeException ex) {
            log.error("CIQ {} [DB-N98] collection_run finalize失败，runUid={}，message={}",
                    api, runUid, ex.getMessage(), ex);
        }
    }

    private CollectionResult success(CollectionContext context,
                                     Instant start,
                                     int requestCount,
                                     long received,
                                     long validCount,
                                     long rejected,
                                     long inserted,
                                     Integer httpStatus) {
        Instant end = Instant.now();
        return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode,
                true, CollectionStatus.SUCCESS, start, end,
                Duration.between(start, end).toMillis(), requestCount, received,
                validCount, 0, inserted, 0, rejected, 0, httpStatus, null, null);
    }

    private CollectionResult partial(CollectionContext context,
                                     Instant start,
                                     int requestCount,
                                     long received,
                                     long validCount,
                                     long rejected,
                                     long inserted,
                                     Integer httpStatus,
                                     String errorCode,
                                     String errorMessage) {
        Instant end = Instant.now();
        return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode,
                false, CollectionStatus.PARTIAL_SUCCESS, start, end,
                Duration.between(start, end).toMillis(), requestCount, received,
                validCount, 0, inserted, 0, rejected, 0, httpStatus,
                errorCode, errorMessage);
    }

    private CollectionResult failure(CollectionContext context,
                                     Instant start,
                                     int requestCount,
                                     long received,
                                     long validCount,
                                     long rejected,
                                     long inserted,
                                     Integer httpStatus,
                                     String errorCode,
                                     String errorMessage) {
        Instant end = Instant.now();
        return new CollectionResult(context.runUid(), ModuleCode.CIQ, collectorCode,
                false, received > 0 ? CollectionStatus.PARTIAL_SUCCESS : CollectionStatus.FAILED,
                start, end, Duration.between(start, end).toMillis(), requestCount,
                received, validCount, 0, inserted, 0, rejected, 1,
                httpStatus, errorCode, errorMessage);
    }
}

package com.mytransitgps.persistence.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.entity.JbRealtimeSnapshotEntity;
import com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity;
import com.mytransitgps.persistence.mapper.JbRealtimeSnapshotMapper;
import com.mytransitgps.persistence.mapper.JbVehicleLatestStateMapper;
import com.mytransitgps.persistence.mapper.JbVehicleObservationMapper;
import com.mytransitgps.persistence.mapper.JbVehicleObservationQcMapper;
import com.mytransitgps.persistence.model.ObservationMappingResult;
import com.mytransitgps.persistence.model.SnapshotPersistenceContext;
import com.mytransitgps.persistence.model.SnapshotPersistenceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 中文名称：Johor Bahru Realtime Snapshot 事务持久化服务。
 *
 * 功能说明：接收重新从磁盘读取的 enriched_full.json，在一个事务中写入
 * realtime_snapshot、vehicle_observation、vehicle_observation_qc 并更新 vehicle_latest_state。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class RealtimePersistenceService {

    private static final Logger log = LoggerFactory.getLogger(RealtimePersistenceService.class);
    private final MyTransitGpsDatabaseProperties properties;
    private final VehicleObservationDatabaseMapper databaseMapper;
    private final JbRealtimeSnapshotMapper snapshotMapper;
    private final JbVehicleObservationMapper observationMapper;
    private final JbVehicleObservationQcMapper qcMapper;
    private final JbVehicleLatestStateMapper latestStateMapper;

    public RealtimePersistenceService(MyTransitGpsDatabaseProperties properties,
                                      VehicleObservationDatabaseMapper databaseMapper,
                                      JbRealtimeSnapshotMapper snapshotMapper,
                                      JbVehicleObservationMapper observationMapper,
                                      JbVehicleObservationQcMapper qcMapper,
                                      JbVehicleLatestStateMapper latestStateMapper) {
        this.properties = properties; this.databaseMapper = databaseMapper; this.snapshotMapper = snapshotMapper;
        this.observationMapper = observationMapper; this.qcMapper = qcMapper; this.latestStateMapper = latestStateMapper;
    }

    @Transactional
    public SnapshotPersistenceResult persist(JsonNode diskRoot, SnapshotPersistenceContext context) {
        // Snapshot、Observation、QC 和 Latest 必须在同一事务内成功或整体回滚。
        long start = System.nanoTime();
        log.info("开始数据库事务，runUid={}，requestUid={}，snapshotUid={}，feedUid={}",
                context.runUid(), context.requestUid(), context.snapshotUid(), context.feedUid());
        try {
        JsonNode snapshot = diskRoot.path("snapshot");
        JbRealtimeSnapshotEntity snapshotRow = mapSnapshot(snapshot, context);
        snapshotMapper.insert(snapshotRow);
        log.info("realtime_snapshot写入完成，snapshotUid={}", context.snapshotUid());

        // 映射器从磁盘 JSON 逐 occurrence 建立数据库行，并保留异常和重复观测。
        ObservationMappingResult mapped = databaseMapper.map(diskRoot, context.feedUid(), context.runUid(),
                context.requestUid(), context.snapshotUid(), context.staticReferences());
        if (!mapped.observations().isEmpty()) observationMapper.insert(mapped.observations(), properties.persistence.getBatchSize());
        log.info("vehicle_observation批量写入完成，snapshotUid={}，rows={}", context.snapshotUid(), mapped.observations().size());
        if (properties.persistence.isSaveQcDetail() && !mapped.qcRows().isEmpty()) qcMapper.insert(mapped.qcRows(), properties.persistence.getQcBatchSize());
        log.info("vehicle_observation_qc写入完成，snapshotUid={}，rows={}", context.snapshotUid(), mapped.qcRows().size());

        int latestCount = 0;
        if (properties.persistence.isUpdateLatestState()) {
            // Latest 是可覆盖的查询加速表；历史 Observation 始终追加保存，不被替换。
            for (JbVehicleLatestStateEntity state : mapped.latestStates()) {
                latestStateMapper.upsert(state);
                latestCount++;
            }
        }
        log.info("vehicle_latest_state UPSERT完成，snapshotUid={}，rows={}", context.snapshotUid(), latestCount);
        long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        log.info("Snapshot数据库事务提交完成，snapshotUid={}，observations={}，qc={}，elapsedMs={}",
                context.snapshotUid(), mapped.observations().size(), mapped.qcRows().size(), elapsed);
        return new SnapshotPersistenceResult(context.snapshotUid(), mapped.observations().size(), mapped.qcRows().size(), latestCount, elapsed);
        } catch (RuntimeException ex) {
            long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
            log.error("Snapshot数据库事务失败并将回滚，runUid={}，requestUid={}，snapshotUid={}，elapsedMs={}，错误信息={}",
                    context.runUid(), context.requestUid(), context.snapshotUid(), elapsed, ex.getMessage());
            throw ex;
        }
    }

    private JbRealtimeSnapshotEntity mapSnapshot(JsonNode node, SnapshotPersistenceContext context) {
        JbRealtimeSnapshotEntity row = new JbRealtimeSnapshotEntity();
        row.uid = context.snapshotUid(); row.feedUid = context.feedUid(); row.runUid = context.runUid(); row.requestUid = context.requestUid();
        row.enrichmentStaticVersionUid = context.staticReferences().staticVersionUid(); row.batchId = text(node, "batch_id");
        row.gtfsRealtimeVersion = text(node, "gtfs_realtime_version"); row.incrementality = text(node, "incrementality");
        row.feedTimestampRaw = longValue(node, "feed_timestamp"); row.feedTime = row.feedTimestampRaw == null ? null : Instant.ofEpochSecond(row.feedTimestampRaw);
        row.entityCount = integer(node, "entity_count"); row.vehicleCount = integer(node, "vehicle_count"); row.uniqueRouteCount = integer(node, "unique_route_count");
        row.routeDirectMatchCount = integer(node, "route_direct_match_count"); row.routeTripFallbackMatchCount = integer(node, "route_trip_fallback_match_count");
        row.routeResolvedCount = integer(node, "route_resolved_count"); row.routeUnresolvedCount = integer(node, "route_unresolved_count");
        row.tripMatchCount = integer(node, "trip_static_match_count"); row.tripUnmatchedCount = integer(node, "trip_static_unmatched_count");
        row.directionMatchCount = integer(node, "direction_match_count"); row.directionMismatchCount = integer(node, "direction_mismatch_count");
        row.directionNotComparableCount = integer(node, "direction_not_comparable_count"); row.responseSha256 = text(node, "response_sha256");
        row.duplicateSnapshot = context.duplicateSnapshot(); row.referencedSnapshotUid = context.referencedSnapshotUid();
        return row;
    }

    private String text(JsonNode node, String name) { JsonNode value = node.get(name); return value == null || value.isNull() ? null : value.asText(); }
    private Integer integer(JsonNode node, String name) { JsonNode value = node.get(name); return value == null || value.isNull() ? null : value.intValue(); }
    private Long longValue(JsonNode node, String name) { JsonNode value = node.get(name); return value == null || value.isNull() ? null : value.longValue(); }
}

package com.mytransitgps.modules.ciqbus.service;

import com.mytransitgps.modules.ciqbus.client.LtaCrossBorderBusClient;
import com.mytransitgps.modules.ciqbus.client.RawHttpResponse;
import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import com.mytransitgps.modules.ciqbus.domain.CiqBusObservation;
import com.mytransitgps.modules.ciqbus.domain.CiqBusPassage;
import com.mytransitgps.modules.ciqbus.domain.CiqBusRouteStop;
import com.mytransitgps.modules.ciqbus.parser.CiqBusRouteStopResolver;
import com.mytransitgps.modules.ciqbus.parser.LtaBusArrivalParser;
import com.mytransitgps.modules.ciqbus.persistence.CiqBusRealtimeRepository;
import com.mytransitgps.modules.ciqbus.redis.CiqBusEventStateStore;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;

public class CiqBusRealtimeService {
    private static final Logger log = LoggerFactory.getLogger(CiqBusRealtimeService.class);

    private final LtaCrossBorderBusClient ltaClient;
    private final LtaBusArrivalParser parser;
    private final CiqBusEventMatcher matcher;
    private final CiqBusEventStateStore stateStore;
    private final CiqBusRealtimeRepository repository;
    private final CiqBusRouteStopResolver stopResolver;
    private final CiqBusProperties properties;
    private final ZoneId zoneId;

    public CiqBusRealtimeService(LtaCrossBorderBusClient ltaClient, LtaBusArrivalParser parser,
                                 CiqBusEventMatcher matcher, CiqBusEventStateStore stateStore,
                                 CiqBusRealtimeRepository repository, CiqBusRouteStopResolver stopResolver,
                                 CiqBusProperties properties) {
        this.ltaClient = ltaClient;
        this.parser = parser;
        this.matcher = matcher;
        this.stateStore = stateStore;
        this.repository = repository;
        this.stopResolver = stopResolver;
        this.properties = properties;
        this.zoneId = ZoneId.of(properties.getTimezone());
    }

    public CycleResult runCycle(String cycleId) {
        Instant collectionTime = Instant.now();
        log.info("[CIQBUS-LTA] 项目进度：采集轮次初始化完成，cycleId={}，collectionTime={}，请求站点数={}，目标线路={}",
                cycleId, collectionTime, properties.getLta().getStopCodes().size(), properties.getLta().getTargetServices());
        int errors = 0;
        List<CiqBusObservation> observations = new ArrayList<>();
        for (String stopCode : properties.getLta().getStopCodes()) {
            try {
                log.info("[CIQBUS-LTA] 项目进度：开始采集站点到站数据，cycleId={}，stopCode={}", cycleId, stopCode);
                RawHttpResponse response = ltaClient.fetchBusArrival(stopCode);
                if (response.statusCode() == 200) {
                    List<CiqBusObservation> parsed = parser.parse(response.body(), collectionTime);
                    observations.addAll(parsed);
                    log.info("[CIQBUS-LTA] 项目进度：站点采集解析成功，cycleId={}，stopCode={}，车辆观测数={}",
                            cycleId, stopCode, parsed.size());
                } else {
                    errors++;
                    log.warn("[CIQBUS-LTA] 项目进度：站点采集失败，cycleId={}，stopCode={}，http={}，errorCode={}",
                            cycleId, stopCode, response.statusCode(), response.errorCode());
                }
            } catch (RuntimeException ex) {
                errors++;
                log.warn("[CIQBUS-LTA] 项目进度：站点采集异常，cycleId={}，stopCode={}，errorType={}，reason={}",
                        cycleId, stopCode, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }
        log.info("[CIQBUS-LTA] 项目进度：全部站点采集完成，cycleId={}，总车辆观测数={}，采集错误数={}",
                cycleId, observations.size(), errors);
        int matched = 0;
        int newEvents = 0;
        int inserted = 0;
        int updated = 0;
        int crossings = 0;
        try {
            for (CiqBusObservation observation : observations) {
                CiqBusEventMatcher.MatchResult match = matcher.match(observation);
                matched++;
                if (match.newEvent()) {
                    newEvents++;
                }
                PassageOutcome outcome = maybeWritePassage(match.event(), observation);
                if (outcome.inserted()) {
                    inserted++;
                } else if (outcome.updated()) {
                    updated++;
                }
                if (outcome.crossingCreated()) {
                    crossings++;
                }
            }
        } catch (RedisConnectionFailureException ex) {
            errors++;
            log.error("[CIQBUS-LTA] 项目进度：Redis不可用，本轮CIQBus事件不做降级写入，reason={}", ex.getMessage(), ex);
        }
        CycleResult result = new CycleResult(cycleId, collectionTime, properties.getLta().getStopCodes().size(),
                observations.size(), matched, newEvents, inserted, updated, crossings, errors);
        log.info("[CIQBUS-LTA] 项目进度：采集轮次完成，time={}，requestStops={}，services={}，observations={}，matched={}，newEvents={}，passagesInserted={}，passagesUpdated={}，crossingsCreated={}，errors={}",
                result.collectionTime(), result.requestStops(), properties.getLta().getTargetServices(),
                result.observations(), result.matched(), result.newEvents(), result.passagesInserted(),
                result.passagesUpdated(), result.crossingsCreated(), result.errors());
        return result;
    }

    private PassageOutcome maybeWritePassage(CiqBusActiveEvent event, CiqBusObservation observation) {
        CiqBusRouteStop stop = stopResolver.resolve(observation).orElse(null);
        if (stop == null || event.getVisitedStops().contains(stop.stopCode())) {
            log.info("[CIQBUS-PASSAGE] 项目进度：跳过过站写入，matchedEventId={}，routeNo={}，directionCode={}，sourceStopCode={}，原因={}",
                    event.getMatchedEventId(), observation.serviceNo(), observation.directionCode(),
                    observation.sourceStopCode(), stop == null ? "未匹配到CIQ站点" : "站点已处理");
            return PassageOutcome.none();
        }
        boolean nearStop = observation.latitude() != null && observation.longitude() != null
                && stop.latitude() != null && stop.longitude() != null
                && CiqBusEventMatcher.haversineKm(observation.latitude(), observation.longitude(), stop.latitude(), stop.longitude()) * 1000
                <= properties.getMatching().getNearStopMeters();
        long etaSeconds = Duration.between(observation.collectionTime(), observation.estimatedArrival()).abs().toSeconds();
        boolean etaNear = etaSeconds <= properties.getMatching().getEtaZeroSeconds();
        if (!nearStop && !etaNear) {
            log.info("[CIQBUS-PASSAGE] 项目进度：车辆未达到过站条件，matchedEventId={}，stopCode={}，nearStop={}，etaSeconds={}",
                    event.getMatchedEventId(), stop.stopCode(), nearStop, etaSeconds);
            return PassageOutcome.none();
        }
        boolean firstRedisHit = stateStore.markPassageIfAbsent(event.getMatchedEventId(), stop.stopCode(),
                Duration.ofMinutes(properties.getRedis().getPassageTtlMinutes()));
        CiqBusPassage passage = new CiqBusPassage(UUID.randomUUID(), null, event.getMatchedEventId(),
                event.getRouteNo(), event.getOperatorCode(), event.getDirectionCode(), "WOODLANDS",
                stop.stopCode(), stop.stopName(), stop.stopSequence(), stop.passageType(), observation.collectionTime(),
                observation.estimatedArrival(), event.getFirstSeenTime(), event.getLastSeenTime(),
                observation.latitude(), observation.longitude(), observation.sourceStopCode(),
                event.getObservationCount(), nearStop ? event.getConfidence() : Math.min(event.getConfidence(), 0.65d),
                nearStop ? "GPS_NEAR_STOP" : "ETA_NEAR_ZERO", observation.sourceName());
        CiqBusRealtimeRepository.PassageWriteResult write = repository.upsertPassage(passage);
        log.info("[CIQBUS-PASSAGE] 项目进度：过站记录写入数据库完成，matchedEventId={}，stopCode={}，passageType={}，首次Redis命中={}，数据库新增={}",
                event.getMatchedEventId(), stop.stopCode(), stop.passageType(), firstRedisHit, write.inserted());
        event.getVisitedStops().add(stop.stopCode());
        if ("ENTRY".equals(stop.passageType())) {
            event.setEntryStopCode(stop.stopCode());
            event.setEntryTime(passage.passTime());
        } else if ("EXIT".equals(stop.passageType())) {
            event.setExitStopCode(stop.stopCode());
            event.setExitTime(passage.passTime());
        }
        stateStore.saveActive(event, Duration.ofMinutes(properties.getRedis().getActiveTtlMinutes()));
        CiqBusRealtimeRepository.CrossingWriteResult crossing = repository.tryCreateCrossing(event.getMatchedEventId(), zoneId);
        if (crossing.inserted()) {
            event.setStatus("COMPLETED");
            stateStore.saveActive(event, Duration.ofMinutes(properties.getRedis().getActiveTtlMinutes()));
            stateStore.markCompleted(crossing.fingerprint(), Duration.ofMinutes(properties.getRedis().getCompletedTtlMinutes()));
            log.info("[CIQBUS-PASSAGE] 项目进度：完整过境事件已生成，matchedEventId={}，fingerprint={}",
                    event.getMatchedEventId(), crossing.fingerprint());
        }
        return new PassageOutcome(write.inserted() && firstRedisHit, !write.inserted(), crossing.inserted());
    }

    public record CycleResult(String cycleId, Instant collectionTime, int requestStops, int observations,
                              int matched, int newEvents, int passagesInserted, int passagesUpdated,
                              int crossingsCreated, int errors) {
    }

    private record PassageOutcome(boolean inserted, boolean updated, boolean crossingCreated) {
        static PassageOutcome none() { return new PassageOutcome(false, false, false); }
    }
}

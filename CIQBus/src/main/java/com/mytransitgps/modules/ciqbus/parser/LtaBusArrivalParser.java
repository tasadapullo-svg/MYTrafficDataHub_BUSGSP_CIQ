package com.mytransitgps.modules.ciqbus.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.modules.ciqbus.domain.CiqBusObservation;
import com.mytransitgps.modules.ciqbus.domain.CiqBusRouteStop;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LtaBusArrivalParser {
    private static final Logger log = LoggerFactory.getLogger(LtaBusArrivalParser.class);

    private final ObjectMapper objectMapper;
    private final Set<String> targetServices;
    private final CiqBusRouteStopResolver resolver;

    public LtaBusArrivalParser(ObjectMapper objectMapper, CiqBusProperties properties, CiqBusRouteStopResolver resolver) {
        this.objectMapper = objectMapper;
        this.targetServices = new HashSet<>(properties.getLta().getTargetServices());
        this.resolver = resolver;
    }

    public List<CiqBusObservation> parse(byte[] body, Instant collectionTime) {
        try {
            log.info("[CIQBUS-PARSE] 项目进度：开始解析LTA BusArrival JSON，collectionTime={}，响应字节={}",
                    collectionTime, body == null ? 0 : body.length);
            JsonNode root = objectMapper.readTree(body);
            String sourceStopCode = root.path("BusStopCode").asText("");
            List<CiqBusObservation> observations = new ArrayList<>();
            JsonNode services = root.path("Services");
            if (!services.isArray()) {
                log.info("[CIQBUS-PARSE] 项目进度：LTA响应没有Services数组，sourceStopCode={}", sourceStopCode);
                return List.of();
            }
            for (JsonNode service : services) {
                String serviceNo = service.path("ServiceNo").asText("");
                if (!targetServices.contains(serviceNo)) {
                    continue;
                }
                String operator = service.path("Operator").asText("");
                addBus(observations, serviceNo, operator, sourceStopCode, service.path("NextBus"), collectionTime);
                addBus(observations, serviceNo, operator, sourceStopCode, service.path("NextBus2"), collectionTime);
                addBus(observations, serviceNo, operator, sourceStopCode, service.path("NextBus3"), collectionTime);
            }
            log.info("[CIQBUS-PARSE] 项目进度：LTA解析完成，sourceStopCode={}，目标线路={}，有效车辆观测数={}",
                    sourceStopCode, targetServices, observations.size());
            return observations;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid LTA BusArrival JSON", ex);
        }
    }

    private void addBus(List<CiqBusObservation> observations, String serviceNo, String operator,
                        String sourceStopCode, JsonNode bus, Instant collectionTime) {
        if (bus == null || bus.isMissingNode()) {
            return;
        }
        Instant estimatedArrival = parseInstant(bus.path("EstimatedArrival").asText(""));
        if (estimatedArrival == null) {
            return;
        }
        Double latitude = parseCoordinate(bus.path("Latitude").asText(""));
        Double longitude = parseCoordinate(bus.path("Longitude").asText(""));
        String ltaDirection = bus.path("VisitNumber").asText("");
        CiqBusObservation unresolved = new CiqBusObservation(serviceNo, operator, null, ltaDirection, sourceStopCode,
                estimatedArrival, latitude, longitude, text(bus, "Load"), text(bus, "Feature"),
                text(bus, "Type"), collectionTime, sourceStopCode, "LTA_BUS_ARRIVAL");
        String direction = resolver.resolve(unresolved).map(CiqBusRouteStop::directionCode).orElse(null);
        if (direction == null) {
            log.info("[CIQBUS-PARSE] 项目进度：车辆方向未匹配到CIQBus配置，routeNo={}，sourceStopCode={}，visitNumber={}",
                    serviceNo, sourceStopCode, ltaDirection);
            return;
        }
        observations.add(new CiqBusObservation(serviceNo, operator, direction, ltaDirection, sourceStopCode,
                estimatedArrival, latitude, longitude, text(bus, "Load"), text(bus, "Feature"),
                text(bus, "Type"), collectionTime, sourceStopCode, "LTA_BUS_ARRIVAL"));
    }

    private String text(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : value;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toInstant();
    }

    private Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        double parsed = Double.parseDouble(value);
        return parsed == 0.0d ? null : parsed;
    }
}

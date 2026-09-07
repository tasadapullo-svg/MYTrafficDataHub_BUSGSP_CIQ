package com.mytransitgps.modules.ciqbus.redis;

import com.mytransitgps.modules.ciqbus.domain.CiqBusActiveEvent;
import java.time.Duration;
import java.util.List;

public interface CiqBusEventStateStore {
    List<CiqBusActiveEvent> findActive(String routeNo, String directionCode);
    void saveActive(CiqBusActiveEvent event, Duration ttl);
    boolean markPassageIfAbsent(String matchedEventId, String stopCode, Duration ttl);
    void markCompleted(String fingerprint, Duration ttl);
    Long ttlSeconds(String key);
}

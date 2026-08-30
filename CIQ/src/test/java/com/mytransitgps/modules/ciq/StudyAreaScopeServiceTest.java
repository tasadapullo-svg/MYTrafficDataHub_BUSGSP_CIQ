package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.modules.ciq.domain.TrafficSpeedScopeMatch;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 CIQ 三层研究区允许同一 Link 产生多条 scope。 */
class StudyAreaScopeServiceTest {
    @Test
    void sameLinkMayMatchThreeNestedZonesAndCoreIsPrimaryPriority() {
        List<TrafficSpeedScopeMatch> matches = List.of(
                new TrafficSpeedScopeMatch(UUID.randomUUID(), "WOODLANDS", "CORE", "WOODLANDS_CORE"),
                new TrafficSpeedScopeMatch(UUID.randomUUID(), "WOODLANDS", "APPROACH", "WOODLANDS_APPROACH_3KM"),
                new TrafficSpeedScopeMatch(UUID.randomUUID(), "WOODLANDS", "CORRIDOR", "WOODLANDS_CORRIDOR_5KM")
        );

        assertEquals(3, matches.size());
        assertEquals(1, matches.get(0).priority());
        assertTrue(matches.stream().anyMatch(match -> match.zoneCode().equals("APPROACH")));
        assertTrue(matches.stream().anyMatch(match -> match.zoneCode().equals("CORRIDOR")));
    }
}


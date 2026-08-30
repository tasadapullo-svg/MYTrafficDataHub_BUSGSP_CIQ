package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** 验证 study_area readiness 由六个明确组合和几何质量决定。 */
class StudyAreaReadyTest {
    @Test
    void readyWhenNoMissingDuplicateOrInvalidCombination() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(0);

        assertTrue(new CiqTrafficSpeedPersistenceService(jdbc, new CiqProperties()).hasRequiredStudyAreas());
    }

    @Test
    void notReadyWhenAnyCombinationIsMissingDuplicateInactiveInvalidOrWrongSrid() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class))).thenReturn(1);

        assertFalse(new CiqTrafficSpeedPersistenceService(jdbc, new CiqProperties()).hasRequiredStudyAreas());
    }
}


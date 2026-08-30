package com.mytransitgps.modules.ciq.domain;
/** CIQ 接口解析后的不可变业务记录。 */
public record RoadWorkRecord(String eventId, java.time.LocalDate startDate, java.time.LocalDate endDate, String svcDept, String roadName, String other) {}

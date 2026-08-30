package com.mytransitgps.modules.ciq.domain;
/** CIQ 接口解析后的不可变业务记录。 */
public record FaultyTrafficLightRecord(String alarmId, String nodeId, Short type, java.time.Instant startDate, java.time.Instant endDate, String message) {}

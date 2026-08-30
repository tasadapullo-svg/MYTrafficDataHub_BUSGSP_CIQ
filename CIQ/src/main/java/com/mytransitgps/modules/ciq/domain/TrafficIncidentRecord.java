package com.mytransitgps.modules.ciq.domain;
/** CIQ 接口解析后的不可变业务记录。 */
public record TrafficIncidentRecord(String type, java.math.BigDecimal latitude, java.math.BigDecimal longitude, String message) {}

package com.mytransitgps.modules.ciq.domain;
/** CIQ 接口解析后的不可变业务记录。 */
public record VmsRecord(String equipmentId, java.math.BigDecimal latitude, java.math.BigDecimal longitude, String message) {}

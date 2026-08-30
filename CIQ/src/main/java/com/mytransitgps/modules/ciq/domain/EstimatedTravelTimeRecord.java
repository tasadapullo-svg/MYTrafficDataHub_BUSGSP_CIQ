package com.mytransitgps.modules.ciq.domain;
/** CIQ 接口解析后的不可变业务记录。 */
public record EstimatedTravelTimeRecord(String name, short direction, String farEndPoint, String startPoint, String endPoint, short estTime) {}

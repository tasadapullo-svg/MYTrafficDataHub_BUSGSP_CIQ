package com.mytransitgps.gtfs.model;

import java.util.List;
import java.util.Map;

/**
 * Static shape 空间质量检查结果，包括零点、越界和距离异常统计。
 */
public record StaticShapeQcResult(
        Map<String, Object> summary,
        List<Map<String, Object>> shapeDetails,
        List<Map<String, Object>> routeShapeLengths) {
}

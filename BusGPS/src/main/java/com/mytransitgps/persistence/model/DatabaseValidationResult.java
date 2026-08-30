package com.mytransitgps.persistence.model;

/**
 * 中文名称：数据库验收结果。
 *
 * 功能说明：提供报告生成后的关键通过条件和精确字段不匹配数量。
 */
public record DatabaseValidationResult(boolean passed, long jsonVehicles, long databaseObservations,
                                       long comparedValues, long fieldMismatches) {
}

package com.mytransitgps.persistence.model;

import java.util.List;

import com.mytransitgps.persistence.entity.JbVehicleLatestStateEntity;
import com.mytransitgps.persistence.entity.JbVehicleObservationEntity;
import com.mytransitgps.persistence.entity.JbVehicleObservationQcEntity;

/**
 * 中文名称：车辆观测数据库映射结果。
 *
 * 功能说明：保存一次磁盘 enriched JSON 映射出的 Observation、标准化 QC 和 Latest State 行。
 */
public record ObservationMappingResult(
        List<JbVehicleObservationEntity> observations,
        List<JbVehicleObservationQcEntity> qcRows,
        List<JbVehicleLatestStateEntity> latestStates) {
}

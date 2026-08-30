package com.mytransitgps.gtfs.model;

/**
 * 单个车辆位置的经纬度、零点和 Feed 边界质检结果。
 */
public record PositionQcResult(
        boolean positionPresent,
        boolean positionWgs84Valid,
        boolean zeroZeroPosition,
        boolean feedBoundsValid,
        String positionQcStatus) {

    public boolean transitPositionValid() {
        return "VALID_POSITION".equals(positionQcStatus);
    }
}

package com.mytransitgps.gtfs.service;

import com.mytransitgps.gtfs.model.FeedSpatialBounds;
import com.mytransitgps.gtfs.model.PositionQcResult;

/**
 * 车辆位置基础质检服务，检查字段缺失、WGS84 合法性、零点和区域边界。
 */
public class PositionQcService {

    public PositionQcResult evaluate(Double latitude, Double longitude, FeedSpatialBounds bounds) {
        // 判断顺序从“不可计算”到“业务越界”，确保每条异常只有明确的主状态。
        if (latitude == null || longitude == null) {
            return new PositionQcResult(false, false, false, false, "MISSING_POSITION");
        }
        if (!isFinite(latitude) || !isFinite(longitude)
                || latitude < -90.0d || latitude > 90.0d
                || longitude < -180.0d || longitude > 180.0d) {
            return new PositionQcResult(true, false, false, false, "INVALID_WGS84");
        }
        if (Double.compare(latitude, 0.0d) == 0 && Double.compare(longitude, 0.0d) == 0) {
            return new PositionQcResult(true, true, true, false, "ZERO_ZERO_POSITION");
        }
        if (bounds != null && bounds.boundsAvailable() && !bounds.contains(latitude, longitude)) {
            return new PositionQcResult(true, true, false, false, "OUT_OF_BOUNDS");
        }
        return new PositionQcResult(true, true, false, true, "VALID_POSITION");
    }

    private boolean isFinite(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite();
    }
}

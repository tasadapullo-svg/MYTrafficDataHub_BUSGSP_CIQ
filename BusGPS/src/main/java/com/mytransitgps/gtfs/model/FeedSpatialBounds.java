package com.mytransitgps.gtfs.model;

/**
 * Feed 的允许空间边界，用于识别越界和异常坐标。
 */
public record FeedSpatialBounds(
        String feedId,
        int totalShapeCount,
        int referencedShapeCount,
        int orphanShapeCount,
        int referencedShapePointCount,
        int stopCount,
        String boundsSource,
        String boundsWarning,
        String staticSha256,
        Double rawMinLat,
        Double rawMaxLat,
        Double rawMinLon,
        Double rawMaxLon,
        double bufferKm,
        Double effectiveMinLat,
        Double effectiveMaxLat,
        Double effectiveMinLon,
        Double effectiveMaxLon) {

    public boolean boundsAvailable() {
        return effectiveMinLat != null
                && effectiveMaxLat != null
                && effectiveMinLon != null
                && effectiveMaxLon != null;
    }

    public boolean contains(double latitude, double longitude) {
        if (!boundsAvailable()) {
            return false;
        }
        return latitude >= effectiveMinLat
                && latitude <= effectiveMaxLat
                && longitude >= effectiveMinLon
                && longitude <= effectiveMaxLon;
    }
}

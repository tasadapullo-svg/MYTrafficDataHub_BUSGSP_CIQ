package com.mytransitgps.gtfs.util;

/**
 * 地理计算工具，提供经纬度合法性和球面距离等通用算法。
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0d;

    private GeoUtils() {
    }

    public static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaLat / 2.0d) * Math.sin(deltaLat / 2.0d)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLon / 2.0d) * Math.sin(deltaLon / 2.0d);
        double c = 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
        return EARTH_RADIUS_M * c;
    }
}

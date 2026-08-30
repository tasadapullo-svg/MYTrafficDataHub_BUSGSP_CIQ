package com.mytransitgps.persistence.typehandler;

import java.util.List;
import java.util.Locale;

/**
 * 中文名称：PostGIS 几何值。
 *
 * 功能说明：保存带 SRID 的 EWKT 文本，明确 Point 坐标顺序为经度在前、纬度在后。
 */
public record PostgisGeometry(String ewkt) {

    public static PostgisGeometry point(double longitude, double latitude) {
        return new PostgisGeometry(String.format(Locale.ROOT, "SRID=4326;POINT(%.12f %.12f)", longitude, latitude));
    }

    public static PostgisGeometry lineString(List<double[]> longitudeLatitudePairs) {
        if (longitudeLatitudePairs == null || longitudeLatitudePairs.size() < 2) {
            return null;
        }
        String coordinates = longitudeLatitudePairs.stream()
                .map(pair -> String.format(Locale.ROOT, "%.12f %.12f", pair[0], pair[1]))
                .reduce((left, right) -> left + "," + right)
                .orElseThrow();
        return new PostgisGeometry("SRID=4326;LINESTRING(" + coordinates + ")");
    }
}

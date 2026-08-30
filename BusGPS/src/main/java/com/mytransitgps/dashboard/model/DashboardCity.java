package com.mytransitgps.dashboard.model;

import java.util.Arrays;

/**
 * BUS GPS 大屏支持的研究城市枚举。
 *
 * <p>统一维护城市代码、数据库 Schema、地图中心点和缩放等级，用于现有 GPS 大屏查询路由。
 */
public enum DashboardCity {
    JB("JB", "johor_bahru", "Johor Bahru", "新山", "jb", 1.4927, 103.7414, 11),
    KUCHING("KUCHING", "kuching", "Kuching", "古晋", "kuching", 1.5533, 110.3592, 11),
    KL("KL", "kuala_lumpur", "Kuala Lumpur", "吉隆坡", "kl", 3.1390, 101.6869, 11),
    MELAKA("MELAKA", "melaka", "Melaka", "马六甲", "melaka", 2.1896, 102.2501, 12);

    private final String code;
    private final String databaseCode;
    private final String nameEn;
    private final String nameZh;
    private final String schema;
    private final double latitude;
    private final double longitude;
    private final int zoom;

    DashboardCity(String code, String databaseCode, String nameEn, String nameZh, String schema,
                  double latitude, double longitude, int zoom) {
        this.code = code;
        this.databaseCode = databaseCode;
        this.nameEn = nameEn;
        this.nameZh = nameZh;
        this.schema = schema;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zoom = zoom;
    }

    public String code() { return code; }
    public String databaseCode() { return databaseCode; }
    public String nameEn() { return nameEn; }
    public String nameZh() { return nameZh; }
    public String schema() { return schema; }
    public double latitude() { return latitude; }
    public double longitude() { return longitude; }
    public int zoom() { return zoom; }

    public static DashboardCity fromCode(String value) {
        if (value == null || value.isBlank()) return JB;
        String normalized = value.trim().replace('-', '_');
        return Arrays.stream(values())
                .filter(city -> city.code.equalsIgnoreCase(normalized)
                        || city.databaseCode.equalsIgnoreCase(normalized)
                        || city.schema.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported dashboard city: " + value));
    }
}

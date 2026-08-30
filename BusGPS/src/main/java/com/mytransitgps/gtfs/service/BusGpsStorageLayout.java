package com.mytransitgps.gtfs.service;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * BUS GPS 文件存储布局。
 *
 * <p>长期运行 JSON 证据统一写入 {@code data_download/BusGPS/yyyyMMdd/...}，
 * 每日归档统一写入 {@code data_download/BusGPS/archive/yyyy/MM/...}，
 * 与 CIQ 的 {@code data_download/CIQ/yyyyMMdd/...} 和
 * {@code data_download/CIQ/archive/yyyy/MM/...} 形成对称的数据域目录。</p>
 *
 * <p>旧版 {@code data_download/json_data/...} 与 {@code data_download/daily_archive/...}
 * 仅用于启动迁移兼容，不再作为任何新数据的写入位置。</p>
 *
 * <p>GTFS-Realtime 原始 Protobuf 仍保留在 {@code data_download/raw_data/...}，
 * 因为该目录属于 RAW 二进制证据层，不属于此前需要迁移的 JSON 数据目录。</p>
 */
public final class BusGpsStorageLayout {

    private static final String BUS_GPS_ROOT = "BusGPS";
    private static final String ARCHIVE_ROOT = "archive";
    private static final String LEGACY_JSON_ROOT = "json_data";
    private static final String LEGACY_ARCHIVE_ROOT = "daily_archive";

    private BusGpsStorageLayout() {
    }

    /** BUS GPS 标准数据根目录。 */
    public static Path jsonRoot(Path workspaceRoot) {
        return workspaceRoot.resolve(BUS_GPS_ROOT);
    }

    /** BUS GPS 指定日期的 JSON 根目录。 */
    public static Path dailyJsonRoot(Path workspaceRoot, String yyyyMMdd) {
        return jsonRoot(workspaceRoot).resolve(yyyyMMdd);
    }

    /** BUS GPS 标准归档根目录。 */
    public static Path archiveRoot(Path workspaceRoot) {
        return jsonRoot(workspaceRoot).resolve(ARCHIVE_ROOT);
    }

    /** BUS GPS 指定年月的标准归档目录。 */
    public static Path archiveRoot(Path workspaceRoot, LocalDate archiveDate) {
        return archiveRoot(workspaceRoot)
                .resolve(Integer.toString(archiveDate.getYear()))
                .resolve(String.format("%02d", archiveDate.getMonthValue()));
    }

    /** 旧版 JSON 根目录，仅用于历史迁移/兼容读取。 */
    public static Path legacyJsonRoot(Path workspaceRoot) {
        return workspaceRoot.resolve(LEGACY_JSON_ROOT);
    }

    /** 旧版指定日期 JSON 根目录，仅用于历史迁移/兼容读取。 */
    public static Path legacyDailyJsonRoot(Path workspaceRoot, String yyyyMMdd) {
        return legacyJsonRoot(workspaceRoot).resolve(yyyyMMdd);
    }

    /** 旧版归档根目录，仅用于历史迁移。 */
    public static Path legacyArchiveRoot(Path workspaceRoot) {
        return workspaceRoot.resolve(LEGACY_ARCHIVE_ROOT);
    }
}

package com.mytransitgps.gtfs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mytransitgps.gtfs.service.BusGpsLegacyStorageMigrator;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证旧 json_data / daily_archive 能迁移到标准 BusGPS 目录且不丢失文件。 */
class BusGpsLegacyStorageMigratorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldMigrateLegacyJsonAndArchiveToBusGpsDomain() throws Exception {
        Path legacyMetadata = tempDir.resolve("json_data/20260830/Johor_Bahru/sample_realtime_metadata_20260830.json");
        Files.createDirectories(legacyMetadata.getParent());
        Files.writeString(legacyMetadata,
                "{\"parsed_json_path\":\"json_data/20260830/Johor_Bahru/sample.json\"}");
        Files.writeString(legacyMetadata.resolveSibling("sample.json"), "{\"ok\":true}");

        Path legacyArchive = tempDir.resolve("daily_archive/2026/08/mytransitgps_daily_20260829.zip");
        Files.createDirectories(legacyArchive.getParent());
        Files.writeString(legacyArchive, "legacy-zip");

        MyTransitGpsDatabaseProperties properties = new MyTransitGpsDatabaseProperties();
        properties.setWorkspaceRoot(tempDir.toString());
        WorkspaceRootResolver resolver = new WorkspaceRootResolver(properties);
        BusGpsLegacyStorageMigrator migrator = new BusGpsLegacyStorageMigrator(resolver);
        var method = BusGpsLegacyStorageMigrator.class.getDeclaredMethod("migrate", Path.class);
        method.setAccessible(true);
        method.invoke(migrator, tempDir);

        Path migratedMetadata = tempDir.resolve("BusGPS/20260830/Johor_Bahru/sample_realtime_metadata_20260830.json");
        assertTrue(Files.exists(migratedMetadata));
        assertTrue(Files.readString(migratedMetadata).contains("BusGPS/20260830/Johor_Bahru/sample.json"));
        assertTrue(Files.exists(tempDir.resolve("BusGPS/archive/2026/08/mytransitgps_daily_20260829.zip")));
        assertFalse(Files.exists(tempDir.resolve("json_data")));
        assertFalse(Files.exists(tempDir.resolve("daily_archive")));
    }
}

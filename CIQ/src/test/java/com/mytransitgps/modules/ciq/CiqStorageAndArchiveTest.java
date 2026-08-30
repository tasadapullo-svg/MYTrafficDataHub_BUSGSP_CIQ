package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.archive.CiqDailyArchiveService;
import com.mytransitgps.modules.ciq.archive.CiqDailyArchiveStatus;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证 CIQ 每日八接口目录、原始 JSON 落盘、ZIP 校验和成功后源目录删除。 */
class CiqStorageAndArchiveTest {
    @TempDir
    Path tempDir;

    @Test
    void createsEightInterfaceFoldersAndArchivesPreviousDay() throws Exception {
        Files.createDirectories(tempDir.resolve("raw_data"));
        Files.createDirectories(tempDir.resolve("json_data"));
        Files.createDirectories(tempDir.resolve("runtime"));

        MyTransitGpsDatabaseProperties workspaceProperties = new MyTransitGpsDatabaseProperties();
        workspaceProperties.setWorkspaceRoot(tempDir.toString());
        WorkspaceRootResolver resolver = new WorkspaceRootResolver(workspaceProperties);
        CiqProperties ciqProperties = new CiqProperties();
        ciqProperties.getStorage().setRootDirectory("CIQ");

        CiqJsonStorageService storage = new CiqJsonStorageService(resolver, ciqProperties);
        LocalDate date = LocalDate.of(2026, 8, 29);
        Path dailyRoot = storage.ensureDailyLayout(date);
        for (CiqApiCode code : CiqApiCode.values()) {
            assertTrue(Files.isDirectory(dailyRoot.resolve(code.folderName())));
        }

        var artifact = storage.saveRawJson(CiqApiCode.API01, date, UUID.randomUUID(), 1,
                Instant.parse("2026-08-29T12:00:00Z"), "{\"value\":[]}".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(artifact.path()));
        assertNotNull(artifact.sha256());

        CiqDailyArchiveService archiveService = new CiqDailyArchiveService(storage, new ObjectMapper());
        var result = archiveService.archiveDate(date, true);
        assertEquals(CiqDailyArchiveStatus.SUCCESS, result.status());
        assertTrue(Files.exists(result.archivePath()));
        assertTrue(Files.exists(Path.of(result.archivePath().toString() + ".sha256")));
        assertFalse(Files.exists(dailyRoot));

        try (ZipFile zip = new ZipFile(result.archivePath().toFile(), StandardCharsets.UTF_8)) {
            String root = "CIQ_20260829/";
            for (CiqApiCode code : CiqApiCode.values()) {
                assertNotNull(zip.getEntry(root + code.folderName() + "/"));
            }
            assertNotNull(zip.getEntry(root + "manifest/daily_manifest.json"));
            assertNotNull(zip.getEntry(root + "manifest/checksums.sha256"));
        }
    }
}

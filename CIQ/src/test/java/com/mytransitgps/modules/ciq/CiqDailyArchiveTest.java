package com.mytransitgps.modules.ciq;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 验证 CIQ 每日归档失败时保留源 JSON 目录。 */
class CiqDailyArchiveTest {
    @TempDir
    Path tempDir;

    @Test
    void archiveFailureKeepsSourceDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve("raw_data"));
        Files.createDirectories(tempDir.resolve("json_data"));
        Files.createDirectories(tempDir.resolve("runtime"));
        MyTransitGpsDatabaseProperties workspace = new MyTransitGpsDatabaseProperties();
        workspace.setWorkspaceRoot(tempDir.toString());
        CiqProperties properties = new CiqProperties();
        properties.getStorage().setRootDirectory("CIQ");
        CiqJsonStorageService storage = new CiqJsonStorageService(new WorkspaceRootResolver(workspace), properties);
        LocalDate date = LocalDate.of(2026, 8, 29);
        storage.saveRawJson(CiqApiCode.API01, date, UUID.randomUUID(), 1, Instant.parse("2026-08-29T10:00:00Z"),
                "{\"value\":[]}".getBytes(StandardCharsets.UTF_8));
        Path source = storage.dailyRoot(date);
        Files.writeString(storage.ciqRoot().resolve("archive"), "blocks archive directory", StandardCharsets.UTF_8);

        var result = new CiqDailyArchiveService(storage, new ObjectMapper()).archiveDate(date, true);

        assertEquals(CiqDailyArchiveStatus.FAILED, result.status());
        assertTrue(Files.exists(source));
        assertTrue(Files.exists(source.resolve(CiqApiCode.API01.folderName())));
    }
}

package com.mytransitgps.modules.ciq.storage;

import com.mytransitgps.common.util.HashUtils;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CIQ 原始 JSON 文件存储服务。
 *
 * <p>统一按照 {@code CIQ/yyyyMMdd/APIxx_接口名/} 结构保存原始响应，并在每日首次写入时创建八个接口目录。
 * 本服务只负责文件证据，不执行数据库业务写入，也不决定研究区域范围。
 */
@Component
public class CiqJsonStorageService {
    private static final Logger log = LoggerFactory.getLogger(CiqJsonStorageService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final WorkspaceRootResolver workspaceRootResolver;
    private final CiqProperties properties;

    public CiqJsonStorageService(WorkspaceRootResolver workspaceRootResolver, CiqProperties properties) {
        this.workspaceRootResolver = workspaceRootResolver;
        this.properties = properties;
    }

    /** 创建指定日期的 CIQ 根目录及八个接口子目录。 */
    public Path ensureDailyLayout(LocalDate date) {
        Path dailyRoot = dailyRoot(date);
        try {
            Files.createDirectories(dailyRoot);
            for (CiqApiCode code : CiqApiCode.values()) {
                Files.createDirectories(dailyRoot.resolve(code.folderName()));
            }
            return dailyRoot;
        } catch (IOException ex) {
            log.error("CIQ每日目录创建失败，date={}，path={}，错误信息={}", date, dailyRoot, ex.getMessage(), ex);
            throw new IllegalStateException("CIQ每日目录创建失败: " + dailyRoot, ex);
        }
    }

    /** 原子保存一页原始 JSON，并返回文件哈希证据。 */
    public CiqRawJsonArtifact saveRawJson(CiqApiCode apiCode, LocalDate date, UUID runUid,
                                          int pageNo, Instant capturedAt, byte[] body) {
        if (body == null) {
            throw new IllegalArgumentException("CIQ原始响应body不能为空");
        }
        Path dailyRoot = ensureDailyLayout(date);
        ZoneId zone = ZoneId.of(properties.getTimezone());
        String ts = TIMESTAMP.format(capturedAt.atZone(zone));
        String prefix = apiCode == CiqApiCode.API01 ? "traffic_speed" : apiCode.folderName();
        String fileName = prefix + "_" + ts + "_" + runUid + "_page_"
                + String.format("%03d", pageNo) + ".json";
        Path finalPath = dailyRoot.resolve(apiCode.folderName()).resolve(fileName);
        Path tempPath = finalPath.resolveSibling(fileName + ".tmp");
        try {
            Files.write(tempPath, body);
            moveAtomically(tempPath, finalPath);
            String sha256 = HashUtils.sha256Hex(body);
            log.info("CIQ原始JSON保存完成，api={}，runUid={}，page={}，bytes={}，sha256={}，path={}",
                    apiCode, runUid, pageNo, body.length, sha256, finalPath);
            return new CiqRawJsonArtifact(apiCode, date, pageNo, finalPath, body.length, sha256);
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException cleanupError) {
                log.warn("CIQ原始JSON临时文件清理失败，path={}，错误信息={}", tempPath, cleanupError.getMessage());
            }
            log.error("CIQ原始JSON保存失败，api={}，runUid={}，page={}，path={}，错误信息={}",
                    apiCode, runUid, pageNo, finalPath, ex.getMessage(), ex);
            throw new IllegalStateException("CIQ原始JSON保存失败: " + finalPath, ex);
        }
    }

    /** API07 等文件型接口保存原始数据文件，仍位于对应 API 当日目录。 */
    public CiqRawJsonArtifact saveDataFile(CiqApiCode apiCode, LocalDate date, UUID runUid,
                                           Instant capturedAt, String suggestedFileName, byte[] body) {
        if (body == null) throw new IllegalArgumentException("CIQ数据文件body不能为空");
        Path dailyRoot = ensureDailyLayout(date);
        String clean = suggestedFileName == null || suggestedFileName.isBlank() ? "traffic_flow_data.bin"
                : suggestedFileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String ts = TIMESTAMP.format(capturedAt.atZone(ZoneId.of(properties.getTimezone())));
        String fileName = "data_" + ts + "_" + runUid + "_" + clean;
        Path finalPath = dailyRoot.resolve(apiCode.folderName()).resolve(fileName);
        Path tempPath = finalPath.resolveSibling(fileName + ".tmp");
        try {
            Files.write(tempPath, body); moveAtomically(tempPath, finalPath);
            String sha256 = HashUtils.sha256Hex(body);
            log.info("CIQ数据文件保存完成，api={}，runUid={}，bytes={}，sha256={}，path={}", apiCode, runUid, body.length, sha256, finalPath);
            return new CiqRawJsonArtifact(apiCode, date, 1, finalPath, body.length, sha256);
        } catch (IOException ex) {
            try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            throw new IllegalStateException("CIQ数据文件保存失败: " + finalPath, ex);
        }
    }

    public Path ciqRoot() {
        String rootDirectory = properties.getStorage().getRootDirectory();
        if (rootDirectory == null || rootDirectory.isBlank()) {
            throw new IllegalStateException("traffic.ciq.storage.root-directory未配置");
        }
        return workspaceRootResolver.resolve().resolve(rootDirectory).normalize();
    }

    public Path dailyRoot(LocalDate date) {
        return ciqRoot().resolve(DATE.format(date));
    }

    public Path archiveRoot(LocalDate date) {
        return ciqRoot().resolve("archive")
                .resolve(Integer.toString(date.getYear()))
                .resolve(String.format("%02d", date.getMonthValue()));
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

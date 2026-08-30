package com.mytransitgps.persistence.service;

import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 统一解析长期运行工作区根目录。
 *
 * <p>支持新的 `TRAFFIC_WORKSPACE_ROOT` 外部数据根，也兼容旧 BUS GPS `raw_data/json_data/runtime`
 * 目录结构，避免目录治理改变既有 BUS GPS 读写行为。</p>
 */
@Component
public class WorkspaceRootResolver {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceRootResolver.class);
    private final MyTransitGpsDatabaseProperties properties;
    private final AtomicBoolean resolvedLogged = new AtomicBoolean();

    public WorkspaceRootResolver(MyTransitGpsDatabaseProperties properties) {
        this.properties = properties;
    }

    public Path resolve() {
        String configured = properties.getWorkspaceRoot();
        if (configured != null && !configured.isBlank()) {
            Path resolved = resolveConfigured(configured);
            logResolved(resolved, "CONFIGURED");
            return resolved;
        }
        Path dataDownload = resolveRelativeWorkspace("data_download");
        if (dataDownload != null) {
            logResolved(dataDownload, "AUTO_DETECT_DATA_DOWNLOAD");
            return dataDownload;
        }
        Path cwd = currentDirectory();
        for (Path cursor = cwd; cursor != null; cursor = cursor.getParent()) {
            if (isWorkspace(cursor)) {
                logResolved(cursor, "AUTO_DETECT");
                return cursor;
            }
        }
        log.error("无法解析 MYTrafficDataHub 工作区根目录，currentDirectory={}", cwd);
        throw new IllegalStateException("TRAFFIC_WORKSPACE_ROOT is required: cannot locate workspace from " + cwd);
    }

    private Path resolveConfigured(String configured) {
        Path configuredPath = Path.of(configured);
        if (configuredPath.isAbsolute()) {
            Path candidate = configuredPath.toAbsolutePath().normalize();
            if (isWorkspace(candidate)) {
                return candidate;
            }
            Path dataDownload = resolveRelativeWorkspace("data_download");
            if (dataDownload != null) {
                log.warn("配置的 MYTrafficDataHub 工作区无效，改用项目内 data_download，candidate={}，fallback={}", candidate, dataDownload);
                return dataDownload;
            }
            return requireWorkspace(candidate);
        }
        Path relativeWorkspace = resolveRelativeWorkspace(configured);
        if (relativeWorkspace != null) {
            return relativeWorkspace;
        }
        return requireWorkspace(configuredPath.toAbsolutePath().normalize());
    }

    private Path resolveRelativeWorkspace(String relativePath) {
        Path configuredPath = Path.of(relativePath);
        if (configuredPath.isAbsolute()) {
            return null;
        }
        Path cwd = currentDirectory();
        for (Path cursor = cwd; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve(configuredPath).normalize();
            if (isWorkspace(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Path currentDirectory() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private Path requireWorkspace(Path candidate) {
        if (!isWorkspace(candidate)) {
            log.error("配置的 MYTrafficDataHub 工作区无效，candidate={}", candidate);
            throw new IllegalStateException("Invalid TRAFFIC_WORKSPACE_ROOT: " + candidate);
        }
        return candidate;
    }

    private void logResolved(Path path, String source) {
        if (resolvedLogged.compareAndSet(false, true)) {
            log.info("MYTrafficDataHub 工作区根目录解析完成，source={}，workspaceRoot={}", source, path);
        }
    }

    private boolean isWorkspace(Path path) {
        return path != null && (isLegacyWorkspace(path) || isLongrunWorkspace(path));
    }

    private boolean isLegacyWorkspace(Path path) {
        return Files.isDirectory(path.resolve("raw_data"))
                && Files.isDirectory(path.resolve("json_data"))
                && Files.isDirectory(path.resolve("runtime"));
    }

    private boolean isLongrunWorkspace(Path path) {
        return Files.isDirectory(path.resolve("BusGPS"))
                && Files.isDirectory(path.resolve("CIQ"))
                && Files.isDirectory(path.resolve("logs"));
    }
}

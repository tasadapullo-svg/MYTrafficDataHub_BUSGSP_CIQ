package com.mytransitgps.persistence.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.gtfs.service.BusGpsStorageLayout;
import com.mytransitgps.gtfs.util.HashUtils;
import com.mytransitgps.persistence.model.LocalStaticArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：本地 Johor Bahru Static 证据解析器。
 *
 * 功能说明：从既有 metadata 中选择最新成功的 mybas-johor Static ZIP，
 * 复算 SHA-256 并确认文件存在；不会在 Realtime 周期中重复下载 Static。
 */
public class LocalStaticArtifactResolver {

    private static final Logger log = LoggerFactory.getLogger(LocalStaticArtifactResolver.class);
    private final ObjectMapper objectMapper;

    public LocalStaticArtifactResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LocalStaticArtifact resolve(Path workspaceRoot, String feedId) {
        // 从历史元数据中选择最新且 SHA/文件均可验证的本地 Static 包，避免重复联网下载。
        log.info("开始检查本地Static版本，feedId={}", feedId);
        Path newJsonRoot = BusGpsStorageLayout.jsonRoot(workspaceRoot);
        Path legacyJsonRoot = BusGpsStorageLayout.legacyJsonRoot(workspaceRoot);
        Candidate candidate = java.util.stream.Stream.of(newJsonRoot, legacyJsonRoot)
                .filter(Files::isDirectory)
                .flatMap(root -> {
                    try {
                        return Files.walk(root);
                    } catch (IOException ex) {
                        log.warn("读取Static元数据目录失败，将跳过，path={}，错误信息={}", root, ex.getMessage());
                        return java.util.stream.Stream.<Path>empty();
                    }
                })
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains("_gtfs_static_metadata_"))
                .map(this::readCandidate)
                .filter(value -> value != null && feedId.equals(value.feedId) && "SUCCESS".equals(value.result))
                .max(Comparator.comparing(value -> value.downloadedAt))
                .orElseThrow(() -> new IllegalStateException("No successful local Static metadata found for " + feedId
                        + " under " + newJsonRoot + " or " + legacyJsonRoot));
        try {
            Path zipPath = workspaceRoot.resolve(candidate.objectPath).normalize();
            if (!Files.isRegularFile(zipPath)) {
                throw new IllegalStateException("Static ZIP does not exist: " + zipPath);
            }
            byte[] bytes = Files.readAllBytes(zipPath);
            String actualSha = HashUtils.sha256Hex(bytes);
            if (!actualSha.equals(candidate.sha256)) {
                throw new IllegalStateException("Static ZIP SHA-256 mismatch: " + zipPath);
            }
            log.info("本地Static证据确认完成，feedId={}，sha256={}，bytes={}", feedId, actualSha, bytes.length);
            return new LocalStaticArtifact(zipPath, candidate.metadataPath, actualSha, bytes.length, candidate.downloadedAt);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to resolve local Static artifact.", ex);
        }
    }

    private Candidate readCandidate(Path path) {
        try {
            JsonNode root = objectMapper.readTree(path.toFile());
            String objectPath = text(root, "static_object_path");
            String sha = text(root, "zip_sha256");
            String received = text(root, "response_received_at");
            if (objectPath == null || sha == null || received == null) {
                return null;
            }
            return new Candidate(path, text(root, "feed_id"), text(root, "result"), objectPath, sha, Instant.parse(received));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : value.asText();
    }

    /** 从单个 Static 元数据文件提取出的候选版本。 */
    private record Candidate(Path metadataPath, String feedId, String result, String objectPath, String sha256, Instant downloadedAt) {
    }
}

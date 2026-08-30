package com.mytransitgps.gtfs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static GTFS 归档服务，保存下载包、元数据及内容校验值。
 */
public class StaticArchiveService {

    private static final Logger log = LoggerFactory.getLogger(StaticArchiveService.class);
    private final JsonOutputService jsonOutputService;

    public StaticArchiveService(JsonOutputService jsonOutputService) {
        this.jsonOutputService = jsonOutputService;
    }

    public StaticArchiveResult archive(Path workspaceRoot, String feedId, String sha256, byte[] zipBytes) throws IOException {
        // Static 包按 SHA 寻址，同一内容只保存一次，同时保留版本是否变化的判断。
        log.info("开始Static GTFS归档，feedId={}，sha256={}，zipBytes={}", feedId, sha256, zipBytes == null ? 0 : zipBytes.length);
        Path objectPath = workspaceRoot.resolve("raw_data")
                .resolve("static")
                .resolve("objects")
                .resolve(sha256.substring(0, 2))
                .resolve(sha256.substring(2, 4))
                .resolve(sha256 + ".zip");
        boolean alreadyExists = Files.exists(objectPath);
        if (!alreadyExists) {
            Files.createDirectories(objectPath.getParent());
            Files.write(objectPath, zipBytes);
        }
        Optional<String> previousSha = findLatestStaticSha(BusGpsStorageLayout.jsonRoot(workspaceRoot), feedId);
        if (previousSha.isEmpty()) {
            previousSha = findLatestStaticSha(BusGpsStorageLayout.legacyJsonRoot(workspaceRoot), feedId);
        }
        boolean changedFromPrevious = previousSha.map(previous -> !previous.equals(sha256)).orElse(true);
        if (alreadyExists) {
            log.info("复用已有Static GTFS对象，feedId={}，sha256={}，path={}", feedId, sha256, objectPath);
        } else {
            log.info("Static GTFS对象保存完成，feedId={}，sha256={}，path={}", feedId, sha256, objectPath);
        }
        if (!changedFromPrevious) {
            log.info("Static GTFS版本与上一版本一致，feedId={}，sha256={}", feedId, sha256);
        }
        return new StaticArchiveResult(objectPath, !alreadyExists, changedFromPrevious, previousSha.orElse(null));
    }

    private Optional<String> findLatestStaticSha(Path jsonRoot, String feedId) throws IOException {
        if (!Files.exists(jsonRoot)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.walk(jsonRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("_gtfs_static_metadata.json")
                            || path.getFileName().toString().contains("_gtfs_static_metadata_"))
                    .sorted(Comparator.reverseOrder())
                    .map(path -> readSha(feedId, path))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst();
        }
    }

    private Optional<String> readSha(String feedId, Path path) {
        try {
            JsonNode root = jsonOutputService.objectMapper().readTree(path.toFile());
            if (feedId.equals(root.path("feed_id").asText())) {
                String sha = root.path("zip_sha256").asText(null);
                if (sha != null && !sha.isBlank()) {
                    return Optional.of(sha);
                }
            }
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("读取历史Static元数据失败，将跳过该文件，feedId={}，path={}，错误信息={}",
                    feedId, path, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Static 内容寻址归档的文件位置和版本变化信息。 */
    public record StaticArchiveResult(
            Path objectPath,
            boolean newlyStored,
            boolean changedFromPrevious,
            String previousSha256) {
    }
}

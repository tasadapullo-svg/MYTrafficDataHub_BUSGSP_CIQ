package com.mytransitgps.gtfs.archive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.mytransitgps.gtfs.service.JsonOutputService;
import com.mytransitgps.gtfs.service.BusGpsStorageLayout;
import com.mytransitgps.gtfs.util.GtfsTime;
import com.mytransitgps.gtfs.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 扫描指定日期的 RAW、JSON、元数据和报告，生成可校验的单日 ZIP 数据包。
 */
public class DailyArchiveService {

    private static final Logger log = LoggerFactory.getLogger(DailyArchiveService.class);
    private final Path workspaceRoot;
    private final JsonOutputService jsonOutputService;

    public DailyArchiveService(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
        this.jsonOutputService = new JsonOutputService();
    }

    public DailyArchiveResult archiveDate(LocalDate archiveDate, boolean allowIncompleteDay, boolean forceRebuild) {
        // 归档始终先写临时文件，完整校验后再原子发布，避免留下半成品 ZIP。
        log.info("开始执行单日数据归档，archiveDate={}，allowIncompleteDay={}，forceRebuild={}，workspaceRoot={}",
                archiveDate, allowIncompleteDay, forceRebuild, workspaceRoot);
        String dateText = GtfsTime.formatBatchDate(archiveDate);
        Path archiveDir = BusGpsStorageLayout.archiveRoot(workspaceRoot, archiveDate);
        Path finalArchivePath = archiveDir.resolve("mytransitgps_daily_" + dateText + ".zip");
        Path tempArchivePath = archiveDir.resolve("mytransitgps_daily_" + dateText + ".zip."
                + Instant.now().toEpochMilli() + "." + Thread.currentThread().getId() + ".tmp");
        Path shaPath = archiveDir.resolve("mytransitgps_daily_" + dateText + ".zip.sha256");

        try {
            Files.createDirectories(archiveDir);
            if (Files.exists(finalArchivePath) && !forceRebuild) {
                log.info("每日归档已存在且未要求重建，archiveDate={}，archive={}", archiveDate, finalArchivePath);
                return new DailyArchiveResult(archiveDate, finalArchivePath, null, Files.size(finalArchivePath), 0, 0, 0, 0, 0, 0, 0, 0, 0, DailyArchiveStatus.ALREADY_EXISTS, List.of(), null);
            }

            // 先建立当天源文件清单，后续 manifest、checksums 和 ZIP 必须使用同一快照。
            DailyScan scan = scanDate(archiveDate);
            log.info("每日归档源文件扫描完成，archiveDate={}，sourceFiles={}，rawPb={}，parsedJson={}，enrichedJson={}，realtimeMetadata={}，staticMetadata={}，reportFiles={}",
                    archiveDate, scan.sourceFileCount, scan.rawPbCount, scan.parsedJsonCount, scan.enrichedJsonCount,
                    scan.realtimeMetadataCount, scan.staticMetadataCount, scan.runReportFileCount);
            if (scan.sourceFileCount == 0 || scan.realtimeMetadataCount == 0) {
                log.warn("每日归档没有足够Realtime数据，archiveDate={}，sourceFiles={}，realtimeMetadata={}",
                        archiveDate, scan.sourceFileCount, scan.realtimeMetadataCount);
                return new DailyArchiveResult(archiveDate, null, null, 0L, scan.realtimeMetadataCount, scan.staticMetadataCount, scan.parsedJsonCount, scan.enrichedJsonCount, scan.rawPbCount, scan.staticObjects.size(), scan.runReportFileCount, scan.sourceFileCount, 0, DailyArchiveStatus.NO_DATA, List.of(), null);
            }

            List<String> warnings = new ArrayList<>();
            if (allowIncompleteDay) {
                warnings.add("INCOMPLETE_DAY_TEST_ARCHIVE");
            }

            Map<String, Object> manifest = buildManifest(archiveDate, scan, warnings);
            List<String> checksums = buildChecksums(scan, archiveDate);
            log.info("开始生成每日归档ZIP，archiveDate={}，sourceFiles={}，tempArchive={}",
                    archiveDate, scan.sourceFileCount, tempArchivePath);
            createZip(tempArchivePath, archiveDate, scan, manifest, checksums);
            // 发布前重新打开 ZIP 校验结构；失败时不会覆盖正式归档。
            verifyZip(tempArchivePath, archiveDate);
            log.info("每日归档ZIP完整性校验通过，archiveDate={}，tempArchive={}", archiveDate, tempArchivePath);
            publish(tempArchivePath, finalArchivePath);
            log.info("每日归档ZIP原子发布完成，archiveDate={}，archive={}", archiveDate, finalArchivePath);

            String zipSha = HashUtils.sha256Hex(Files.readAllBytes(finalArchivePath));
            Files.writeString(shaPath, zipSha + "  " + finalArchivePath.getFileName() + System.lineSeparator(), StandardCharsets.UTF_8);
            CleanupSummary cleanup = cleanupArchivedSources(archiveDate, scan);
            if (cleanup.failedDeletes > 0) {
                warnings.add("SOURCE_CLEANUP_FAILED:" + cleanup.failedDeletes);
            }
            DailyArchiveStatus status = warnings.isEmpty() ? DailyArchiveStatus.SUCCESS : DailyArchiveStatus.SUCCESS_WITH_WARNINGS;
            log.info("每日归档完成，archiveDate={}，status={}，sourceFiles={}，archiveEntries={}，archiveBytes={}，sha256={}",
                    archiveDate, status, scan.sourceFileCount, scan.sourceFileCount + 3, Files.size(finalArchivePath), zipSha);
            return new DailyArchiveResult(
                    archiveDate,
                    finalArchivePath,
                    zipSha,
                    Files.size(finalArchivePath),
                    scan.realtimeMetadataCount,
                    scan.staticMetadataCount,
                    scan.parsedJsonCount,
                    scan.enrichedJsonCount,
                    scan.rawPbCount,
                    scan.staticObjects.size(),
                    scan.runReportFileCount,
                    scan.sourceFileCount,
                    scan.sourceFileCount + 3,
                    status,
                    List.copyOf(warnings),
                    null);
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(tempArchivePath);
            } catch (IOException cleanupEx) {
                log.warn("清理失败的每日归档临时文件未成功，path={}，错误信息={}", tempArchivePath, cleanupEx.getMessage());
            }
            log.error("每日归档执行失败，archiveDate={}，错误信息={}", archiveDate, ex.getMessage(), ex);
            return new DailyArchiveResult(archiveDate, null, null, 0L, 0, 0, 0, 0, 0, 0, 0, 0, 0, DailyArchiveStatus.FAILED, List.of(), ex.getMessage());
        }
    }

    private DailyScan scanDate(LocalDate archiveDate) throws IOException {
        String dateText = GtfsTime.formatBatchDate(archiveDate);
        List<ArchiveSourceFile> files = new ArrayList<>();
        Set<Path> staticObjects = new LinkedHashSet<>();
        Map<String, FeedSummaryAccumulator> feedSummaries = new LinkedHashMap<>();
        Set<String> runIds = new LinkedHashSet<>();
        long totalBytes = 0L;
        int parsedJsonCount = 0;
        int enrichedJsonCount = 0;
        int realtimeMetadataCount = 0;
        int staticMetadataCount = 0;
        int rawPbCount = 0;
        int runReportFileCount = 0;
        String firstRequest = null;
        String lastRequest = null;

        Path jsonRoot = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, dateText);
        Path legacyJsonRoot = BusGpsStorageLayout.legacyDailyJsonRoot(workspaceRoot, dateText);
        Path rawRoot = workspaceRoot.resolve("raw_data").resolve(dateText);
        Path legacyRealtimeRawRoot = workspaceRoot.resolve("raw_data").resolve("realtime").resolve(dateText);
        Path reportRoot = workspaceRoot.resolve("run_reports").resolve(dateText);

        for (JsonScanRoot scanRoot : List.of(
                new JsonScanRoot(jsonRoot, "BusGPS/" + dateText + "/"),
                new JsonScanRoot(legacyJsonRoot, "json_data/" + dateText + "/"))) {
            if (!Files.exists(scanRoot.path())) {
                continue;
            }
            for (Path path : listFiles(scanRoot.path())) {
                if (exclude(path)) {
                    continue;
                }
                files.add(new ArchiveSourceFile(path, scanRoot.archivePrefix()
                        + scanRoot.path().relativize(path).toString().replace('\\', '/')));
                totalBytes += Files.size(path);
                String filename = path.getFileName().toString();
                if (filename.contains("_parsed_full_")) {
                    parsedJsonCount++;
                } else if (filename.contains("_enriched_full_")) {
                    enrichedJsonCount++;
                }
                if (filename.contains("_realtime_metadata_")) {
                    realtimeMetadataCount++;
                    MetadataRollup metadataRollup = readMetadata(path);
                    collectFeedSummary(feedSummaries, metadataRollup, true);
                    runIds.addAll(metadataRollup.runIds());
                    firstRequest = minText(firstRequest, metadataRollup.requestStartedAt());
                    lastRequest = maxText(lastRequest, metadataRollup.responseReceivedAt());
                } else if (filename.contains("_static_metadata_")) {
                    staticMetadataCount++;
                    MetadataRollup metadataRollup = readMetadata(path);
                    collectFeedSummary(feedSummaries, metadataRollup, false);
                    runIds.addAll(metadataRollup.runIds());
                    Path staticObject = resolveStaticObject(metadataRollup.staticObjectPath());
                    if (staticObject != null && Files.exists(staticObject)) {
                        staticObjects.add(staticObject);
                    }
                }
            }
        }

        if (Files.exists(rawRoot)) {
            for (Path path : listFiles(rawRoot)) {
                if (exclude(path) || !path.getFileName().toString().endsWith(".pb")) {
                    continue;
                }
                files.add(new ArchiveSourceFile(path, "raw_data/" + dateText + "/" + rawRoot.relativize(path).toString().replace('\\', '/')));
                totalBytes += Files.size(path);
                rawPbCount++;
            }
        }

        if (Files.exists(legacyRealtimeRawRoot)) {
            for (Path path : listFiles(legacyRealtimeRawRoot)) {
                if (exclude(path) || !path.getFileName().toString().endsWith(".pb")) continue;
                files.add(new ArchiveSourceFile(path, "raw_data/realtime/" + dateText + "/" + legacyRealtimeRawRoot.relativize(path).toString().replace('\\', '/')));
                totalBytes += Files.size(path); rawPbCount++;
            }
        }

        if (Files.exists(reportRoot)) {
            for (Path path : listFiles(reportRoot)) {
                if (exclude(path)) {
                    continue;
                }
                files.add(new ArchiveSourceFile(path, "run_reports/" + dateText + "/" + reportRoot.relativize(path).toString().replace('\\', '/')));
                totalBytes += Files.size(path);
                runReportFileCount++;
            }
        }

        Path validationRoot = workspaceRoot.resolve("outputs").resolve("db_validation");
        if (Files.exists(validationRoot)) {
            try (var runDirectories = Files.list(validationRoot)) {
                for (Path runDirectory : runDirectories.filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().contains(dateText)).toList()) {
                    for (Path path : listFiles(runDirectory)) {
                        if (exclude(path)) continue;
                        files.add(new ArchiveSourceFile(path, "outputs/db_validation/" + validationRoot.relativize(path).toString().replace('\\', '/')));
                        totalBytes += Files.size(path); runReportFileCount++;
                    }
                }
            }
        }

        for (Path staticObject : staticObjects) {
            String relative = workspaceRoot.resolve("raw_data").resolve("static").resolve("objects").relativize(staticObject).toString().replace('\\', '/');
            files.add(new ArchiveSourceFile(staticObject, "static/objects/" + relative));
            totalBytes += Files.size(staticObject);
        }

        files.sort(Comparator.comparing(ArchiveSourceFile::archiveRelativePath));
        return new DailyScan(
                files,
                staticObjects,
                parsedJsonCount,
                enrichedJsonCount,
                realtimeMetadataCount,
                staticMetadataCount,
                rawPbCount,
                runReportFileCount,
                files.size(),
                totalBytes,
                buildFeedSummary(feedSummaries),
                List.copyOf(runIds),
                firstRequest,
                lastRequest);
    }

    private MetadataRollup readMetadata(Path metadataPath) throws IOException {
        JsonNode root = jsonOutputService.objectMapper().readTree(metadataPath.toFile());
        return new MetadataRollup(
                root.path("feed_id").asText(null),
                root.path("run_id").asText(null),
                root.path("request_started_at").asText(null),
                root.path("response_received_at").asText(null),
                root.path("result").asText(null),
                root.path("parsed_json_path").asText(null),
                root.path("enriched_json_path").asText(null),
                root.path("raw_pb_path").asText(null),
                root.path("static_object_path").asText(null));
    }

    private void collectFeedSummary(Map<String, FeedSummaryAccumulator> feedSummaries, MetadataRollup metadata, boolean realtime) {
        if (metadata.feedId() == null || metadata.feedId().isBlank()) {
            return;
        }
        FeedSummaryAccumulator accumulator = feedSummaries.computeIfAbsent(metadata.feedId(), FeedSummaryAccumulator::new);
        if (realtime) {
            accumulator.realtimeRequestCount++;
            if ("SUCCESS".equals(metadata.result())) {
                accumulator.successfulRealtimeRequests++;
            } else {
                accumulator.failedRealtimeRequests++;
            }
            if (metadata.parsedJsonPath() != null) {
                accumulator.parsedJsonCount++;
            }
            if (metadata.enrichedJsonPath() != null) {
                accumulator.enrichedJsonCount++;
            }
            if (metadata.rawPbPath() != null) {
                accumulator.rawPbCount++;
            }
        } else {
            accumulator.staticMetadataCount++;
        }
        accumulator.firstRequest = minText(accumulator.firstRequest, metadata.requestStartedAt());
        accumulator.lastRequest = maxText(accumulator.lastRequest, metadata.responseReceivedAt());
    }

    private List<Map<String, Object>> buildFeedSummary(Map<String, FeedSummaryAccumulator> feedSummaries) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FeedSummaryAccumulator accumulator : feedSummaries.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("feed_id", accumulator.feedId);
            row.put("request_count", accumulator.realtimeRequestCount);
            row.put("successful_requests", accumulator.successfulRealtimeRequests);
            row.put("failed_requests", accumulator.failedRealtimeRequests);
            row.put("static_request_count", accumulator.staticMetadataCount);
            row.put("parsed_json_files", accumulator.parsedJsonCount);
            row.put("enriched_json_files", accumulator.enrichedJsonCount);
            row.put("protobuf_files", accumulator.rawPbCount);
            row.put("first_request", accumulator.firstRequest);
            row.put("last_request", accumulator.lastRequest);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildManifest(LocalDate archiveDate, DailyScan scan, List<String> warnings) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("archive_version", "1.0");
        manifest.put("project", "MYTransitGPS");
        manifest.put("timezone", GtfsTime.MALAYSIA_ZONE.getId());
        manifest.put("archive_date", archiveDate.toString());
        manifest.put("archive_created_at_local", Instant.now().atZone(GtfsTime.MALAYSIA_ZONE).toString());
        manifest.put("archive_created_at_utc", Instant.now().toString());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("realtime_request_metadata", scan.realtimeMetadataCount);
        summary.put("static_request_metadata", scan.staticMetadataCount);
        summary.put("parsed_json_files", scan.parsedJsonCount);
        summary.put("enriched_json_files", scan.enrichedJsonCount);
        summary.put("protobuf_objects", scan.rawPbCount);
        summary.put("static_objects", scan.staticObjects.size());
        summary.put("run_report_files", scan.runReportFileCount);
        summary.put("source_file_count", scan.sourceFileCount);
        summary.put("archive_entry_count", scan.sourceFileCount + 3);
        summary.put("uncompressed_bytes", scan.totalBytes);
        manifest.put("summary", summary);
        manifest.put("feed_summary", scan.feedSummary);
        manifest.put("runs", scan.runIds);
        manifest.put("first_request", scan.firstRequest);
        manifest.put("last_request", scan.lastRequest);
        manifest.put("warnings", warnings);
        return manifest;
    }

    private List<String> buildChecksums(DailyScan scan, LocalDate archiveDate) throws IOException {
        List<String> lines = new ArrayList<>();
        for (ArchiveSourceFile file : scan.files) {
            lines.add(HashUtils.sha256Hex(Files.readAllBytes(file.sourcePath())) + "  " + rootFolder(archiveDate) + "/" + file.archiveRelativePath());
        }
        return lines;
    }

    private void createZip(Path tempArchivePath, LocalDate archiveDate, DailyScan scan, Map<String, Object> manifest, List<String> checksums) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(tempArchivePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            for (ArchiveSourceFile file : scan.files) {
                writeEntry(out, rootFolder(archiveDate) + "/" + file.archiveRelativePath(), Files.readAllBytes(file.sourcePath()));
            }
            writeEntry(out, rootFolder(archiveDate) + "/manifest/daily_manifest.json", jsonOutputService.objectMapper().writeValueAsBytes(manifest));
            writeEntry(out, rootFolder(archiveDate) + "/manifest/checksums.sha256", String.join(System.lineSeparator(), checksums).concat(System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            writeEntry(out, rootFolder(archiveDate) + "/README.txt", buildReadme(archiveDate, scan).getBytes(StandardCharsets.UTF_8));
        }
    }

    private CleanupSummary cleanupArchivedSources(LocalDate archiveDate, DailyScan scan) {
        String dateText = GtfsTime.formatBatchDate(archiveDate);
        Path normalizedWorkspace = workspaceRoot.toAbsolutePath().normalize();
        Set<Path> parents = new LinkedHashSet<>();
        int deleted = 0;
        int failed = 0;

        for (ArchiveSourceFile file : scan.files) {
            Path source = file.sourcePath().toAbsolutePath().normalize();
            if (!source.startsWith(normalizedWorkspace) || !isDateScopedSource(source, dateText)) {
                continue;
            }
            try {
                Path parent = source.getParent();
                Files.deleteIfExists(source);
                deleted++;
                if (parent != null) {
                    parents.add(parent);
                }
            } catch (IOException ex) {
                failed++;
                log.warn("每日归档源文件删除失败，archiveDate={}，path={}，error={}", archiveDate, source, ex.getMessage());
            }
        }

        pruneEmptyDirectories(parents, normalizedWorkspace);
        log.info("每日归档源文件清理完成，archiveDate={}，deletedFiles={}，failedDeletes={}", archiveDate, deleted, failed);
        return new CleanupSummary(deleted, failed);
    }

    private boolean isDateScopedSource(Path source, String dateText) {
        Path jsonRoot = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, dateText).toAbsolutePath().normalize();
        Path legacyJsonRoot = BusGpsStorageLayout.legacyDailyJsonRoot(workspaceRoot, dateText).toAbsolutePath().normalize();
        Path rawRoot = workspaceRoot.resolve("raw_data").resolve(dateText).toAbsolutePath().normalize();
        Path legacyRealtimeRawRoot = workspaceRoot.resolve("raw_data").resolve("realtime").resolve(dateText).toAbsolutePath().normalize();
        Path reportRoot = workspaceRoot.resolve("run_reports").resolve(dateText).toAbsolutePath().normalize();
        Path validationRoot = workspaceRoot.resolve("outputs").resolve("db_validation").toAbsolutePath().normalize();
        return source.startsWith(jsonRoot)
                || source.startsWith(legacyJsonRoot)
                || source.startsWith(rawRoot)
                || source.startsWith(legacyRealtimeRawRoot)
                || source.startsWith(reportRoot)
                || source.startsWith(validationRoot) && source.toString().contains(dateText);
    }

    private void pruneEmptyDirectories(Set<Path> parents, Path normalizedWorkspace) {
        List<Path> orderedParents = parents.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .sorted(Comparator.comparingInt((Path path) -> path.getNameCount()).reversed())
                .toList();
        for (Path parent : orderedParents) {
            Path current = parent;
            while (current != null && current.startsWith(normalizedWorkspace) && !current.equals(normalizedWorkspace)) {
                try (var stream = Files.list(current)) {
                    if (stream.findAny().isPresent()) {
                        break;
                    }
                } catch (IOException ex) {
                    break;
                }
                try {
                    Files.deleteIfExists(current);
                } catch (IOException ex) {
                    break;
                }
                current = current.getParent();
            }
        }
    }

    private void verifyZip(Path tempArchivePath, LocalDate archiveDate) throws IOException {
        try (ZipFile zipFile = new ZipFile(tempArchivePath.toFile())) {
            if (zipFile.size() <= 0) {
                throw new IOException("ZIP entry count is zero.");
            }
            if (zipFile.getEntry(rootFolder(archiveDate) + "/manifest/daily_manifest.json") == null) {
                throw new IOException("daily_manifest.json is missing.");
            }
            if (zipFile.getEntry(rootFolder(archiveDate) + "/manifest/checksums.sha256") == null) {
                throw new IOException("checksums.sha256 is missing.");
            }
            ZipEntry checksumsEntry = zipFile.getEntry(rootFolder(archiveDate) + "/manifest/checksums.sha256");
            String checksums = new String(zipFile.getInputStream(checksumsEntry).readAllBytes(), StandardCharsets.UTF_8);
            for (String line : checksums.lines().filter(value -> !value.isBlank()).toList()) {
                String[] parts = line.split("  ", 2);
                if (parts.length != 2) throw new IOException("Invalid checksum manifest line: " + line);
                ZipEntry source = zipFile.getEntry(parts[1]);
                if (source == null) throw new IOException("Archive source entry is missing: " + parts[1]);
                String actual = HashUtils.sha256Hex(zipFile.getInputStream(source).readAllBytes());
                if (!actual.equals(parts[0])) throw new IOException("Archive source checksum mismatch: " + parts[1]);
            }
        }
    }

    private void publish(Path tempArchivePath, Path finalArchivePath) throws IOException {
        try {
            Files.move(tempArchivePath, finalArchivePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempArchivePath, finalArchivePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeEntry(ZipOutputStream out, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        out.write(content);
        out.closeEntry();
    }

    private List<Path> listFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).toList();
        }
    }

    private boolean exclude(Path path) {
        String normalized = path.toString().replace('\\', '/');
        String filename = path.getFileName().toString();
        return normalized.contains("/BusGPS/archive/")
                || normalized.contains("/daily_archive/")
                || normalized.contains("/weekly_archive/")
                || filename.endsWith(".zip")
                || filename.endsWith(".zip.tmp")
                || filename.endsWith(".sha256")
                || filename.endsWith(".tmp")
                || filename.endsWith(".part")
                || filename.endsWith(".lock");
    }

    private Path resolveStaticObject(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return workspaceRoot.resolve(relativePath.replace('/', java.io.File.separatorChar));
    }

    private String rootFolder(LocalDate archiveDate) {
        return "MYTransitGPS_" + GtfsTime.formatBatchDate(archiveDate);
    }

    private String buildReadme(LocalDate archiveDate, DailyScan scan) {
        return """
                MYTransitGPS daily archive
                Archive date: %s
                Timezone: %s
                Realtime metadata: %d
                Static metadata: %d
                Parsed JSON: %d
                Enriched JSON: %d
                RAW protobuf: %d
                Static objects: %d
                Run report files: %d
                """.formatted(
                archiveDate,
                GtfsTime.MALAYSIA_ZONE.getId(),
                scan.realtimeMetadataCount,
                scan.staticMetadataCount,
                scan.parsedJsonCount,
                scan.enrichedJsonCount,
                scan.rawPbCount,
                scan.staticObjects.size(),
                scan.runReportFileCount);
    }

    private String minText(String left, String right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) <= 0 ? left : right;
    }

    private String maxText(String left, String right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    /** 单个归档源文件及其在 ZIP 内的相对路径。 */
    private record JsonScanRoot(Path path, String archivePrefix) {
    }

    private record ArchiveSourceFile(Path sourcePath, String archiveRelativePath) {
    }

    /** 源文件清理的成功与失败数量汇总。 */
    private record CleanupSummary(int deletedFiles, int failedDeletes) {
    }

    /** 从 Realtime 元数据汇总出的请求、Feed 和时间范围。 */
    private record MetadataRollup(
            String feedId,
            String runId,
            String requestStartedAt,
            String responseReceivedAt,
            String result,
            String parsedJsonPath,
            String enrichedJsonPath,
            String rawPbPath,
            String staticObjectPath) {
        private List<String> runIds() {
            return runId == null || runId.isBlank() ? List.of() : List.of(runId);
        }
    }

    /** 构建 manifest 时按 Feed 累加文件和请求统计。 */
    private static class FeedSummaryAccumulator {
        private final String feedId;
        private int realtimeRequestCount;
        private int successfulRealtimeRequests;
        private int failedRealtimeRequests;
        private int staticMetadataCount;
        private int parsedJsonCount;
        private int enrichedJsonCount;
        private int rawPbCount;
        private String firstRequest;
        private String lastRequest;

        private FeedSummaryAccumulator(String feedId) {
            this.feedId = feedId;
        }
    }

    /** 指定日期的不可变扫描结果，作为归档创建和校验的统一输入。 */
    private record DailyScan(
            List<ArchiveSourceFile> files,
            Set<Path> staticObjects,
            int parsedJsonCount,
            int enrichedJsonCount,
            int realtimeMetadataCount,
            int staticMetadataCount,
            int rawPbCount,
            int runReportFileCount,
            int sourceFileCount,
            long totalBytes,
            List<Map<String, Object>> feedSummary,
            List<String> runIds,
            String firstRequest,
            String lastRequest) {
    }
}

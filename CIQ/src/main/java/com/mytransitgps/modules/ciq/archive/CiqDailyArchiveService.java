package com.mytransitgps.modules.ciq.archive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CIQ 每日 JSON ZIP 归档服务。
 *
 * <p>归档前一天的 {@code CIQ/yyyyMMdd} 目录，校验 ZIP 内全部源文件后才删除原日期目录。
 * 归档失败时保留源文件，避免长期科研采集证据丢失。
 */
@Component
public class CiqDailyArchiveService {
    private static final Logger log = LoggerFactory.getLogger(CiqDailyArchiveService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CiqJsonStorageService storageService;
    private final ObjectMapper objectMapper;

    public CiqDailyArchiveService(CiqJsonStorageService storageService, ObjectMapper objectMapper) {
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    /** 将指定日期 CIQ 文件归档为单个 ZIP，并在校验成功后删除源日期目录。 */
    public CiqDailyArchiveResult archiveDate(LocalDate archiveDate, boolean forceRebuild) {
        Path sourceRoot = null;
        Path finalArchive = null;
        Path tempArchive = null;
        try {
            sourceRoot = storageService.dailyRoot(archiveDate);
            Path archiveRoot = storageService.archiveRoot(archiveDate);
            finalArchive = archiveRoot.resolve("CIQ_daily_" + DATE.format(archiveDate) + ".zip");
            Path sidecar = Path.of(finalArchive.toString() + ".sha256");
            tempArchive = Path.of(finalArchive.toString() + ".tmp");
            if (!Files.exists(sourceRoot)) {
                if (Files.exists(finalArchive)) {
                    return resultForExisting(archiveDate, finalArchive, CiqDailyArchiveStatus.ALREADY_EXISTS);
                }
                log.info("CIQ每日归档无源目录，archiveDate={}，source={}", archiveDate, sourceRoot);
                return new CiqDailyArchiveResult(archiveDate, null, null, 0, 0, 0,
                        CiqDailyArchiveStatus.NO_DATA, null);
            }
            List<Path> sourceFiles = listFiles(sourceRoot);
            if (sourceFiles.isEmpty()) {
                log.warn("CIQ每日归档目录存在但没有文件，archiveDate={}，source={}", archiveDate, sourceRoot);
                return new CiqDailyArchiveResult(archiveDate, null, null, 0, 0, 0,
                        CiqDailyArchiveStatus.NO_DATA, null);
            }
            if (Files.exists(finalArchive) && !forceRebuild) {
                return resultForExisting(archiveDate, finalArchive, CiqDailyArchiveStatus.ALREADY_EXISTS);
            }
            Files.createDirectories(archiveRoot);
            Files.deleteIfExists(tempArchive);
            if (forceRebuild) {
                Files.deleteIfExists(finalArchive);
                Files.deleteIfExists(sidecar);
            }

            Map<String, String> checksums = new LinkedHashMap<>();
            for (Path source : sourceFiles) {
                checksums.put(relativeEntry(sourceRoot, source), sha256(source));
            }
            log.info("CIQ每日归档开始生成ZIP，archiveDate={}，sourceFiles={}，tempArchive={}",
                    archiveDate, sourceFiles.size(), tempArchive);
            createZip(tempArchive, archiveDate, sourceRoot, sourceFiles, checksums);
            verifyZip(tempArchive, archiveDate, sourceFiles.size(), checksums);
            moveAtomically(tempArchive, finalArchive);
            String zipSha256 = sha256(finalArchive);
            Files.writeString(sidecar, zipSha256 + "  " + finalArchive.getFileName() + System.lineSeparator(), StandardCharsets.UTF_8);
            deleteTree(sourceRoot);
            int deletedFiles = sourceFiles.size();
            long archiveBytes = Files.size(finalArchive);
            log.info("CIQ每日归档完成，archiveDate={}，sourceFiles={}，deletedFiles={}，archiveBytes={}，sha256={}，archive={}",
                    archiveDate, sourceFiles.size(), deletedFiles, archiveBytes, zipSha256, finalArchive);
            return new CiqDailyArchiveResult(archiveDate, finalArchive, zipSha256, archiveBytes,
                    sourceFiles.size(), deletedFiles, CiqDailyArchiveStatus.SUCCESS, null);
        } catch (Exception ex) {
            if (tempArchive != null) {
                try {
                    Files.deleteIfExists(tempArchive);
                } catch (IOException cleanupError) {
                    log.warn("CIQ每日归档临时ZIP清理失败，path={}，错误信息={}", tempArchive, cleanupError.getMessage());
                }
            }
            log.error("CIQ每日归档失败，archiveDate={}，source={}，错误信息={}",
                    archiveDate, sourceRoot, ex.getMessage(), ex);
            return new CiqDailyArchiveResult(archiveDate, null, null, 0, 0, 0,
                    CiqDailyArchiveStatus.FAILED, ex.getMessage());
        }
    }

    private CiqDailyArchiveResult resultForExisting(LocalDate date, Path archive, CiqDailyArchiveStatus status) throws IOException {
        String sha = sha256(archive);
        return new CiqDailyArchiveResult(date, archive, sha, Files.size(archive), 0, 0, status, null);
    }

    private void createZip(Path target, LocalDate date, Path sourceRoot, List<Path> sourceFiles,
                           Map<String, String> checksums) throws IOException {
        String zipRoot = "CIQ_" + DATE.format(date) + "/";
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target), StandardCharsets.UTF_8)) {
            for (CiqApiCode code : CiqApiCode.values()) {
                addDirectory(out, zipRoot + code.folderName() + "/");
            }
            for (Path source : sourceFiles) {
                String entryName = zipRoot + relativeEntry(sourceRoot, source);
                addFile(out, source, entryName);
            }
            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("archive_version", "1.0");
            manifest.put("archive_date", date.toString());
            manifest.put("created_at", Instant.now().toString());
            manifest.put("source_file_count", sourceFiles.size());
            manifest.put("interfaces", java.util.Arrays.stream(CiqApiCode.values()).map(CiqApiCode::name).toList());
            addBytes(out, zipRoot + "manifest/daily_manifest.json",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest));
            StringBuilder checksumText = new StringBuilder();
            checksums.forEach((path, sha) -> checksumText.append(sha).append("  ").append(path).append(System.lineSeparator()));
            addBytes(out, zipRoot + "manifest/checksums.sha256", checksumText.toString().getBytes(StandardCharsets.UTF_8));
            addBytes(out, zipRoot + "README.txt",
                    ("MYTrafficDataHub CIQ daily archive\narchive_date=" + date + "\nsource_file_count=" + sourceFiles.size() + "\n")
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    private void verifyZip(Path archive, LocalDate date, int sourceFileCount, Map<String, String> checksums) throws IOException {
        String zipRoot = "CIQ_" + DATE.format(date) + "/";
        try (ZipFile zip = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            for (CiqApiCode code : CiqApiCode.values()) {
                if (zip.getEntry(zipRoot + code.folderName() + "/") == null) {
                    throw new IOException("CIQ归档缺少接口目录: " + code.folderName());
                }
            }
            for (Map.Entry<String, String> expected : checksums.entrySet()) {
                ZipEntry entry = zip.getEntry(zipRoot + expected.getKey());
                if (entry == null) {
                    throw new IOException("CIQ归档缺少源文件: " + expected.getKey());
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    String actual = sha256(in);
                    if (!expected.getValue().equals(actual)) {
                        throw new IOException("CIQ归档文件SHA-256不一致: " + expected.getKey());
                    }
                }
            }
            if (checksums.size() != sourceFileCount) {
                throw new IOException("CIQ归档源文件计数不一致");
            }
            if (zip.getEntry(zipRoot + "manifest/daily_manifest.json") == null
                    || zip.getEntry(zipRoot + "manifest/checksums.sha256") == null) {
                throw new IOException("CIQ归档缺少manifest");
            }
        }
        log.info("CIQ每日归档ZIP完整性校验通过，archiveDate={}，sourceFiles={}", date, sourceFileCount);
    }

    private static List<Path> listFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).sorted().forEach(files::add);
        }
        return files;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        List<Path> paths;
        try (var stream = Files.walk(root)) {
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            Files.deleteIfExists(path);
        }
    }

    private static String relativeEntry(Path root, Path source) {
        return root.relativize(source).toString().replace('\\', '/');
    }

    private static void addDirectory(ZipOutputStream out, String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        out.closeEntry();
    }

    private static void addFile(ZipOutputStream out, Path source, String name) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        Files.copy(source, out);
        out.closeEntry();
    }

    private static void addBytes(ZipOutputStream out, String name, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        out.write(bytes);
        out.closeEntry();
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return sha256(in);
        }
    }

    private static String sha256(InputStream input) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK缺少SHA-256算法", ex);
        }
    }
}

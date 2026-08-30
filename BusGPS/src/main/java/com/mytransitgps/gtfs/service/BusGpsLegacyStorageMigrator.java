package com.mytransitgps.gtfs.service;

import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * BUS GPS 旧版文件布局一次性兼容迁移器。
 *
 * <p>应用启动后，如果工作区仍存在旧目录 {@code json_data} 或 {@code daily_archive}，
 * 自动把历史数据迁移到标准目录：</p>
 *
 * <pre>
 * json_data/yyyyMMdd/...  -> BusGPS/yyyyMMdd/...
 * json_data/full/*.json   -> BusGPS/yyyyMMdd/&lt;city&gt;/...
 * json_data/raw/*.pb      -> raw_data/yyyyMMdd/&lt;city&gt;/...
 * daily_archive/yyyy/MM   -> BusGPS/archive/yyyy/MM
 * </pre>
 *
 * <p>迁移是幂等且不覆盖不同内容文件：目标已存在且 SHA-256 相同时删除旧副本；
 * 内容不同时保留目标，并给旧文件追加 conflict 后缀后迁移，避免科研数据丢失。</p>
 */
@Component
@ConditionalOnProperty(prefix = "traffic.bus-gps", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BusGpsLegacyStorageMigrator {

    private static final Logger log = LoggerFactory.getLogger(BusGpsLegacyStorageMigrator.class);
    private static final Pattern DATE_IN_NAME = Pattern.compile("(20\\d{6})");
    private static final Pattern DATE_DIRECTORY = Pattern.compile("20\\d{6}");
    private static final DateTimeFormatter CONFLICT_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final WorkspaceRootResolver workspaceRootResolver;

    public BusGpsLegacyStorageMigrator(WorkspaceRootResolver workspaceRootResolver) {
        this.workspaceRootResolver = workspaceRootResolver;
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public void migrateOnStartup(ApplicationReadyEvent event) {
        Path workspaceRoot = workspaceRootResolver.resolve();
        try {
            MigrationSummary summary = migrate(workspaceRoot);
            if (summary.movedFiles() > 0 || summary.duplicateFilesRemoved() > 0 || summary.rewrittenTextFiles() > 0) {
                log.info("BUS GPS历史存储迁移完成，movedFiles={}，duplicateFilesRemoved={}，rewrittenTextFiles={}，conflicts={}，workspaceRoot={}",
                        summary.movedFiles(), summary.duplicateFilesRemoved(), summary.rewrittenTextFiles(), summary.conflicts(), workspaceRoot);
            } else {
                log.info("BUS GPS历史存储无需迁移，标准目录已生效，workspaceRoot={}", workspaceRoot);
            }
        } catch (Exception ex) {
            // 迁移失败不能静默继续，否则旧数据可能长期停留在错误目录。
            log.error("BUS GPS历史存储迁移失败，workspaceRoot={}，错误信息={}", workspaceRoot, ex.getMessage(), ex);
            throw new IllegalStateException("BUS GPS历史存储迁移失败: " + workspaceRoot, ex);
        }
    }

    /** 供单元测试和维护脚本调用的迁移入口。 */
    MigrationSummary migrate(Path workspaceRoot) throws IOException {
        Files.createDirectories(BusGpsStorageLayout.jsonRoot(workspaceRoot));

        int moved = 0;
        int duplicates = 0;
        int rewritten = 0;
        int conflicts = 0;

        Path legacyJsonRoot = BusGpsStorageLayout.legacyJsonRoot(workspaceRoot);
        if (Files.exists(legacyJsonRoot)) {
            List<Path> children;
            try (var stream = Files.list(legacyJsonRoot)) {
                children = stream.sorted().toList();
            }
            for (Path child : children) {
                String name = child.getFileName().toString();
                if (Files.isDirectory(child) && "CIQ".equalsIgnoreCase(name)) {
                    // 极早期若曾把 CIQ 原始文件放在 json_data/CIQ，下沉到标准 CIQ 根目录，避免误迁到 BusGPS。
                    MoveCounters counters = moveTree(child, workspaceRoot.resolve("CIQ"));
                    moved += counters.moved(); duplicates += counters.duplicates(); conflicts += counters.conflicts();
                    continue;
                }
                if (Files.isDirectory(child) && DATE_DIRECTORY.matcher(name).matches()) {
                    MoveCounters counters = moveTree(child, BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, name));
                    moved += counters.moved(); duplicates += counters.duplicates(); conflicts += counters.conflicts();
                    continue;
                }
                if (Files.isDirectory(child) && "full".equalsIgnoreCase(name)) {
                    MoveCounters counters = migrateLegacyFullJson(child, workspaceRoot);
                    moved += counters.moved(); duplicates += counters.duplicates(); conflicts += counters.conflicts();
                    continue;
                }
                if (Files.isDirectory(child) && "raw".equalsIgnoreCase(name)) {
                    MoveCounters counters = migrateLegacyRawPb(child, workspaceRoot);
                    moved += counters.moved(); duplicates += counters.duplicates(); conflicts += counters.conflicts();
                    continue;
                }
                // 未识别的旧文件不丢弃，集中放入 BusGPS/legacy_unclassified。
                Path target = BusGpsStorageLayout.jsonRoot(workspaceRoot).resolve("legacy_unclassified").resolve(name);
                MoveResult result = movePreservingData(child, target);
                moved += result.moved(); duplicates += result.duplicateRemoved(); conflicts += result.conflict();
            }
            deleteIfEmptyRecursively(legacyJsonRoot);
        }

        Path legacyArchive = BusGpsStorageLayout.legacyArchiveRoot(workspaceRoot);
        if (Files.exists(legacyArchive)) {
            MoveCounters counters = moveTree(legacyArchive, BusGpsStorageLayout.archiveRoot(workspaceRoot));
            moved += counters.moved(); duplicates += counters.duplicates(); conflicts += counters.conflicts();
            deleteIfEmptyRecursively(legacyArchive);
        }

        // 迁移完成后统一修正历史 metadata / manifest 中仍引用 json_data/ 的文本路径。
        Path busGpsRoot = BusGpsStorageLayout.jsonRoot(workspaceRoot);
        if (Files.exists(busGpsRoot)) {
            try (var stream = Files.walk(busGpsRoot)) {
                for (Path file : stream.filter(Files::isRegularFile)
                        .filter(BusGpsLegacyStorageMigrator::isTextMetadataCandidate)
                        .toList()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    String updated = text.replace("json_data/", "BusGPS/")
                            .replace("json_data\\\\", "BusGPS\\\\");
                    if (!updated.equals(text)) {
                        Files.writeString(file, updated, StandardCharsets.UTF_8);
                        rewritten++;
                    }
                }
            }
        }

        return new MigrationSummary(moved, duplicates, rewritten, conflicts);
    }

    private MoveCounters migrateLegacyFullJson(Path sourceRoot, Path workspaceRoot) throws IOException {
        MoveCounters total = new MoveCounters(0, 0, 0);
        for (Path source : listRegularFiles(sourceRoot)) {
            String fileName = source.getFileName().toString();
            String date = dateFromName(fileName);
            String city = cityFromName(fileName);
            Path target = BusGpsStorageLayout.dailyJsonRoot(workspaceRoot, date).resolve(city).resolve(fileName);
            total = total.plus(movePreservingData(source, target));
        }
        deleteIfEmptyRecursively(sourceRoot);
        return total;
    }

    private MoveCounters migrateLegacyRawPb(Path sourceRoot, Path workspaceRoot) throws IOException {
        MoveCounters total = new MoveCounters(0, 0, 0);
        for (Path source : listRegularFiles(sourceRoot)) {
            String fileName = source.getFileName().toString();
            String date = dateFromName(fileName);
            String city = cityFromName(fileName);
            Path target = workspaceRoot.resolve("raw_data").resolve(date).resolve(city).resolve(fileName);
            total = total.plus(movePreservingData(source, target));
        }
        deleteIfEmptyRecursively(sourceRoot);
        return total;
    }

    private MoveCounters moveTree(Path sourceRoot, Path targetRoot) throws IOException {
        MoveCounters total = new MoveCounters(0, 0, 0);
        if (!Files.exists(sourceRoot)) return total;
        for (Path source : listRegularFiles(sourceRoot)) {
            Path relative = sourceRoot.relativize(source);
            Path target = targetRoot.resolve(relative);
            total = total.plus(movePreservingData(source, target));
        }
        deleteIfEmptyRecursively(sourceRoot);
        return total;
    }

    private MoveResult movePreservingData(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            MoveCounters counters = moveTree(source, target);
            return new MoveResult(counters.moved(), counters.duplicates(), counters.conflicts());
        }
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            moveAtomically(source, target);
            return new MoveResult(1, 0, 0);
        }
        if (sameContent(source, target)) {
            Files.deleteIfExists(source);
            return new MoveResult(0, 1, 0);
        }
        String original = target.getFileName().toString();
        String suffix = ".legacy_conflict_" + CONFLICT_TS.format(LocalDateTime.now());
        Path conflictTarget = target.resolveSibling(original + suffix);
        moveAtomically(source, conflictTarget);
        log.warn("BUS GPS历史迁移发现同名不同内容文件，已保留两个版本，target={}，legacyConflict={}", target, conflictTarget);
        return new MoveResult(1, 0, 1);
    }

    private static List<Path> listRegularFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static void deleteIfEmptyRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        List<Path> paths;
        try (var stream = Files.walk(root)) {
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            if (!Files.isDirectory(path)) continue;
            try (var children = Files.list(path)) {
                if (children.findAny().isEmpty()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static boolean sameContent(Path left, Path right) throws IOException {
        if (Files.size(left) != Files.size(right)) return false;
        return sha256(left).equals(sha256(right));
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK缺少SHA-256算法", ex);
        }
    }

    private static String dateFromName(String fileName) {
        Matcher matcher = DATE_IN_NAME.matcher(fileName);
        return matcher.find() ? matcher.group(1) : "legacy_unknown_date";
    }

    private static String cityFromName(String fileName) {
        String value = fileName.toLowerCase(Locale.ROOT);
        if (value.contains("johor")) return "Johor_Bahru";
        if (value.contains("kuala_lumpur") || value.contains("rapid_bus_kl") || value.contains("mrt_feeder")) return "Kuala_Lumpur";
        if (value.contains("kuching")) return "Kuching";
        if (value.contains("melaka")) return "Melaka";
        return "Legacy";
    }

    private static boolean isTextMetadataCandidate(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".txt");
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    record MigrationSummary(int movedFiles, int duplicateFilesRemoved, int rewrittenTextFiles, int conflicts) {
    }

    private record MoveCounters(int moved, int duplicates, int conflicts) {
        MoveCounters plus(MoveResult result) {
            return new MoveCounters(moved + result.moved(), duplicates + result.duplicateRemoved(), conflicts + result.conflict());
        }
    }

    private record MoveResult(int moved, int duplicateRemoved, int conflict) {
    }
}

package com.mytransitgps.gtfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

final class TestArtifactSupport {

    private TestArtifactSupport() {
    }

    static Path workspaceRoot() {
        return fixtureRoot();
    }

    static Path latestLegacyRealtimePb() throws IOException {
        return latestFile(fixtureRoot().resolve("json_data").resolve("raw"), ".pb");
    }

    static Path latestLegacyParsedJson() throws IOException {
        return latestFile(fixtureRoot().resolve("json_data").resolve("full"), ".json");
    }

    static Path latestLegacyStaticZip() throws IOException {
        return latestFile(fixtureRoot().resolve("data").resolve("demo").resolve("jb").resolve("static").resolve("raw"), ".zip");
    }

    static Path oneHourRunReportRoot() {
        return fixtureRoot().resolve("run_reports").resolve("20260827").resolve("one_hour_run_20260827_142338_d4d7");
    }

    static Path oneHourJsonRoot() {
        return fixtureRoot().resolve("json_data").resolve("20260827");
    }

    static Path oneHourRawRoot() {
        return fixtureRoot().resolve("raw_data").resolve("20260827");
    }

    static Path latestFile(Path root, String suffix) throws IOException {
        if (!Files.exists(root)) {
            throw new IllegalStateException("Required fixture root does not exist: " + root);
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted(Comparator.comparing(TestArtifactSupport::lastModifiedTime).reversed())
                    .toList();
            if (files.isEmpty()) {
                throw new IllegalStateException("No fixture files found under " + root + " with suffix " + suffix);
            }
            return files.get(0);
        }
    }

    private static Path fixtureRoot() {
        Path reactorRoot = reactorRoot();
        for (Path candidate : List.of(
                reactorRoot.resolve("test-data").resolve("busgps"),
                reactorRoot,
                reactorRoot.getParent() == null ? reactorRoot : reactorRoot.getParent().resolve("MYTrafficDataHub_DATA_BACKUP_20260830")
        )) {
            if (hasBusGpsFixtures(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("BUS GPS fixtures not found relative to reactor root: " + reactorRoot);
    }

    private static Path reactorRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path cursor = current; cursor != null; cursor = cursor.getParent()) {
            if (Files.exists(cursor.resolve("pom.xml"))
                    && Files.isDirectory(cursor.resolve("Common"))
                    && Files.isDirectory(cursor.resolve("BusGPS"))
                    && Files.isDirectory(cursor.resolve("CIQ"))
                    && Files.isDirectory(cursor.resolve("Application"))) {
                return cursor;
            }
        }
        return current;
    }

    private static boolean hasBusGpsFixtures(Path root) {
        return root != null
                && Files.isDirectory(root.resolve("json_data").resolve("raw"))
                && Files.isDirectory(root.resolve("json_data").resolve("full"))
                && Files.isDirectory(root.resolve("data").resolve("demo").resolve("jb").resolve("static").resolve("raw"))
                && Files.isDirectory(root.resolve("json_data").resolve("20260827"))
                && Files.isDirectory(root.resolve("raw_data").resolve("20260827"))
                && Files.isDirectory(root.resolve("run_reports").resolve("20260827").resolve("one_hour_run_20260827_142338_d4d7"));
    }

    private static java.nio.file.attribute.FileTime lastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read file timestamp: " + path, ex);
        }
    }
}


package com.mytransitgps.modules.ciqbus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CiqBusLegacyRemovalTest {
    @Test
    void removedRuntimeHttpSourceIsAbsentFromMainCodeAndConfig() throws Exception {
        String legacyName = "Causeway" + "Link";
        List<String> terms = List.of(legacyName, legacyName.toLowerCase(), "CW" + "1", "CW" + "2",
                "CW" + "3", "CW" + "4", "CW" + "5", "CW" + "6", "CW" + "7");
        Path cwd = Path.of("").toAbsolutePath();
        Path root = "CIQBus".equals(cwd.getFileName().toString())
                ? cwd.resolve("src").resolve("main")
                : cwd.resolve("CIQBus").resolve("src").resolve("main");
        long count;
        try (var stream = Files.walk(root)) {
            count = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".yml"))
                    .filter(p -> containsAny(p, terms))
                    .count();
        }
        assertEquals(0, count);
    }

    private boolean containsAny(Path path, List<String> terms) {
        try {
            String text = Files.readString(path);
            return terms.stream().anyMatch(text::contains);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}

package com.mytransitgps.common.util;

import java.nio.file.Path;

/** Resolves traffic platform log files from the project-local log root. */
public final class TrafficLogPathResolver {
    private TrafficLogPathResolver() {
    }

    public static Path infoLog(Path workspaceRoot) {
        String configured = System.getenv("TRAFFIC_FILE_LOG_ROOT");
        Path logRoot = configured == null || configured.isBlank() ? Path.of("logs") : Path.of(configured);
        if (!logRoot.isAbsolute()) {
            logRoot = workspaceRoot.resolve(logRoot);
        }
        return logRoot.normalize().resolve("info").resolve("mytransitgps-info.log");
    }
}

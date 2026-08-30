package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.service.WorkspaceRootResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceRootResolverTest {

    @TempDir Path temp;

    @Test
    void resolvesExplicitIdeaWorkspace() throws Exception {
        Files.createDirectories(temp.resolve("raw_data"));
        Files.createDirectories(temp.resolve("json_data"));
        Files.createDirectories(temp.resolve("runtime"));
        MyTransitGpsDatabaseProperties properties = new MyTransitGpsDatabaseProperties();
        properties.setWorkspaceRoot(temp.toString());

        assertThat(new WorkspaceRootResolver(properties).resolve()).isEqualTo(temp.toAbsolutePath().normalize());
    }

    @Test
    void resolvesRelativeDataDownloadFromParentWorkspace() throws Exception {
        Path project = temp.resolve("project");
        Path dataDownload = project.resolve("data_download");
        Files.createDirectories(dataDownload.resolve("BusGPS"));
        Files.createDirectories(dataDownload.resolve("CIQ"));
        Files.createDirectories(dataDownload.resolve("logs"));
        Path module = project.resolve("Application");
        Files.createDirectories(module);
        MyTransitGpsDatabaseProperties properties = new MyTransitGpsDatabaseProperties();
        properties.setWorkspaceRoot("data_download");

        String originalUserDir = System.getProperty("user.dir");
        try {
            System.setProperty("user.dir", module.toString());
            assertThat(new WorkspaceRootResolver(properties).resolve()).isEqualTo(dataDownload.toAbsolutePath().normalize());
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }

    @Test
    void fallsBackToProjectDataDownloadForWrongExplicitWorkspace() {
        MyTransitGpsDatabaseProperties properties = new MyTransitGpsDatabaseProperties();
        properties.setWorkspaceRoot(temp.toString());

        assertThat(new WorkspaceRootResolver(properties).resolve().toString()).endsWith("data_download");
    }
}

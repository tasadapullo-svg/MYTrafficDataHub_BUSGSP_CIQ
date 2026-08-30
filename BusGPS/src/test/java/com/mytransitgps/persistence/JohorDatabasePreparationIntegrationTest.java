package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.service.DatabaseConnectionService;
import com.mytransitgps.persistence.service.DatabaseFeedService;
import com.mytransitgps.persistence.service.JbStaticPersistenceService;
import com.mytransitgps.persistence.service.LocalStaticArtifactResolver;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 显式数据库预检：只核验连接并按需导入一次本地 Johor Static，不访问Realtime API。 */
@Tag("db-preflight")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "mytransitgps.db-test.enabled=false", "app.redis.connect-on-startup=false"})
class JohorDatabasePreparationIntegrationTest {
    @Autowired DatabaseConnectionService connection;
    @Autowired DatabaseFeedService feeds;
    @Autowired JbStaticPersistenceService staticPersistence;
    @Autowired ObjectMapper objectMapper;

    @Test void verifiesDatabaseAndPreparesStaticOnce() {
        assumeTrue(Boolean.getBoolean("mytransitgps.db-preflight.confirm"));
        assertThat(connection.verify().database()).isEqualTo("postgres");
        var feed = feeds.requireFeed("mybas-johor");
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        var artifact = new LocalStaticArtifactResolver(objectMapper).resolve(workspace, "mybas-johor");
        var prepared = staticPersistence.prepare(workspace, feed.uid, "mybas-johor", artifact);
        assertThat(prepared.references().routeUids()).hasSize(21);
        assertThat(prepared.references().tripUids()).hasSize(1782);
        assertThat(prepared.references().stopUids()).hasSize(988);
    }
}

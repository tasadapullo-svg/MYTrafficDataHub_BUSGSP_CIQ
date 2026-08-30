package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.config.MyTransitGpsDatabaseProperties;
import com.mytransitgps.persistence.model.PreparedDatabaseStatic;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import com.mytransitgps.persistence.service.DatabaseFeedService;
import com.mytransitgps.persistence.service.JbStaticPersistenceService;
import com.mytransitgps.persistence.service.LocalStaticArtifactResolver;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 显式准备五 Feed 独立 Static 版本；只使用数据库已有版本或本地已校验 ZIP。 */
@Tag("multicity-static-prepare")
@SpringBootTest(properties = {"mytransitgps.database.enabled=true", "app.redis.connect-on-startup=false"})
class MulticityStaticPreparationIntegrationTest {
    @Autowired DatabaseFeedService feedService;
    @Autowired MyTransitGpsDatabaseProperties properties;
    @Autowired JbStaticPersistenceService staticService;
    @Autowired CitySchemaRouter router;
    @Autowired ObjectMapper objectMapper;

    @Test void preparesFiveFeedSpecificStaticVersionsFromLocalEvidence() {
        assumeTrue(Boolean.getBoolean("mytransitgps.multicity-static.confirm"), "需要显式确认 Static 准备");
        Path workspace = Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent();
        var resolver = new LocalStaticArtifactResolver(objectMapper);
        var contexts = feedService.requireContexts(properties.multicityDbTest.getFeeds());
        for (var feed : contexts) {
            var artifact = resolver.resolve(workspace, feed.feedId());
            PreparedDatabaseStatic prepared = router.withSchema(feed.schemaName(),
                    () -> staticService.prepare(workspace, feed.feedUid(), feed.feedId(), artifact));
            assertThat(prepared.references().staticVersionUid()).isNotNull();
            assertThat(prepared.references().staticSha256()).isEqualTo(artifact.sha256());
        }
    }
}

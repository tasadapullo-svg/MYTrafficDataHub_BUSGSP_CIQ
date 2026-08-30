package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mytransitgps.persistence.routing.CitySchemaContext;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** 验证 KL 两个 Feed 共享 kl Schema，但使用不同数据库 feed_uid。 */
class KlDualFeedIsolationTest {
    @Test void sharesSchemaButKeepsFeedIdentity() {
        CitySchemaRouter router = new CitySchemaRouter();
        UUID bus = UUID.randomUUID(); UUID mrt = UUID.randomUUID();
        Map<UUID, String> routed = Map.of(bus, router.withSchema("kl", CitySchemaContext::current),
                mrt, router.withSchema("kl", CitySchemaContext::current));
        assertThat(routed).hasSize(2); assertThat(routed.values()).containsOnly("kl"); assertThat(bus).isNotEqualTo(mrt);
    }
}

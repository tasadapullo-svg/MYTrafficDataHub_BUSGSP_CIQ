package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mytransitgps.persistence.routing.CitySchemaContext;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import org.junit.jupiter.api.Test;

/** 验证城市 Schema 固定白名单与 finally 清理。 */
class FeedSchemaRouterTest {
    private final CitySchemaRouter router = new CitySchemaRouter();

    @Test void acceptsOnlyFrozenCitySchemasAndClearsContext() {
        assertThat(router.withSchema("kuching", CitySchemaContext::current)).isEqualTo("kuching");
        assertThat(CitySchemaContext.current()).isNull();
        assertThatThrownBy(() -> router.withSchema("public", () -> { }))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("UNSUPPORTED_SCHEMA");
    }
}

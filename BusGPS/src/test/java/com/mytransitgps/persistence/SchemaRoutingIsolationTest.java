package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.mytransitgps.persistence.routing.CitySchemaContext;
import com.mytransitgps.persistence.routing.CitySchemaRouter;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

/** 验证并发线程的城市上下文不会串库。 */
class SchemaRoutingIsolationTest {
    @Test void isolatesJohorKuchingAndMelakaThreads() throws Exception {
        CitySchemaRouter router = new CitySchemaRouter();
        var executor = Executors.newFixedThreadPool(3);
        try {
            Callable<String> jb = () -> router.withSchema("jb", CitySchemaContext::current);
            Callable<String> kuching = () -> router.withSchema("kuching", CitySchemaContext::current);
            Callable<String> melaka = () -> router.withSchema("melaka", CitySchemaContext::current);
            assertThat(executor.invokeAll(java.util.List.of(jb, kuching, melaka)).stream().map(f -> {
                try { return f.get(); } catch (Exception ex) { throw new RuntimeException(ex); }
            }).toList()).containsExactlyInAnyOrder("jb", "kuching", "melaka");
        } finally { executor.shutdownNow(); }
        assertThat(CitySchemaContext.current()).isNull();
    }
}

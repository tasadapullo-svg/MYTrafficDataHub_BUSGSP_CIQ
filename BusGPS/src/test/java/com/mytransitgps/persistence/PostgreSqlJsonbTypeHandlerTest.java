package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.persistence.typehandler.PostgreSqlJsonbTypeHandler;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

class PostgreSqlJsonbTypeHandlerTest {
    @Test void preservesJsonTypesOnWriteAndRead() throws Exception {
        var handler = new PostgreSqlJsonbTypeHandler(); var json = new ObjectMapper().readTree("{\"n\":1,\"s\":\"1\",\"z\":null}");
        PreparedStatement ps = mock(PreparedStatement.class); handler.setNonNullParameter(ps, 1, json, null);
        verify(ps).setObject(org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.argThat(v -> v instanceof PGobject p && "jsonb".equals(p.getType())));
        PGobject value = new PGobject(); value.setType("jsonb"); value.setValue(json.toString());
        ResultSet rs = mock(ResultSet.class); when(rs.getObject("payload")).thenReturn(value);
        assertThat(handler.getNullableResult(rs, "payload")).isEqualTo(json);
    }
}

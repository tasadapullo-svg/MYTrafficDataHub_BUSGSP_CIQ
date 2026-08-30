package com.mytransitgps.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.mytransitgps.persistence.typehandler.PostgisGeometry;
import com.mytransitgps.persistence.typehandler.PostgreSqlGeometryTypeHandler;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

class PostgreSqlGeometryTypeHandlerTest {
    @Test void writesLongitudeBeforeLatitudeWithSrid4326() throws Exception {
        var handler = new PostgreSqlGeometryTypeHandler(); var point = PostgisGeometry.point(103.761, 1.492);
        assertThat(point.ewkt()).isEqualTo("SRID=4326;POINT(103.761000000000 1.492000000000)");
        PreparedStatement ps = mock(PreparedStatement.class); handler.setNonNullParameter(ps, 2, point, null);
        verify(ps).setObject(org.mockito.ArgumentMatchers.eq(2), org.mockito.ArgumentMatchers.argThat(v -> v instanceof PGobject p && "geometry".equals(p.getType()) && point.ewkt().equals(p.getValue())));
        PGobject pg = new PGobject(); pg.setType("geometry"); pg.setValue(point.ewkt()); ResultSet rs = mock(ResultSet.class); when(rs.getObject(1)).thenReturn(pg);
        assertThat(handler.getNullableResult(rs, 1)).isEqualTo(point);
    }
}

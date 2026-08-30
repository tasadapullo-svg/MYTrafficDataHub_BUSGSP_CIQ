package com.mytransitgps.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/**
 * 中文名称：PostgreSQL PostGIS Geometry 类型处理器。
 *
 * 功能说明：通过 EWKT 在 Java 与 PostGIS geometry 之间传递几何值，
 * 不修改数据库 SRID、字段类型或任何 Schema 定义。
 */
public class PostgreSqlGeometryTypeHandler extends BaseTypeHandler<PostgisGeometry> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PostgisGeometry parameter, JdbcType jdbcType) throws SQLException {
        PGobject value = new PGobject();
        value.setType("geometry");
        value.setValue(parameter.ewkt());
        ps.setObject(i, value);
    }

    @Override
    public PostgisGeometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convert(rs.getObject(columnName));
    }

    @Override
    public PostgisGeometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convert(rs.getObject(columnIndex));
    }

    @Override
    public PostgisGeometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convert(cs.getObject(columnIndex));
    }

    private PostgisGeometry convert(Object value) {
        if (value == null) {
            return null;
        }
        String text = value instanceof PGobject pgObject ? pgObject.getValue() : value.toString();
        return text == null ? null : new PostgisGeometry(text);
    }
}

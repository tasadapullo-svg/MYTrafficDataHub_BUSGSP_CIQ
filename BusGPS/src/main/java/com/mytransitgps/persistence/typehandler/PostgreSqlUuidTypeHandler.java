package com.mytransitgps.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * 中文名称：PostgreSQL UUID 类型处理器。
 *
 * 功能说明：显式注册 Java UUID 与 PostgreSQL uuid 的双向映射，兼容当前 MyBatis-Plus Boot 4 组合。
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class PostgreSqlUuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) throws SQLException { ps.setObject(i, parameter); }
    @Override public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException { return uuid(rs.getObject(columnName)); }
    @Override public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException { return uuid(rs.getObject(columnIndex)); }
    @Override public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException { return uuid(cs.getObject(columnIndex)); }
    private UUID uuid(Object value) { return value == null ? null : value instanceof UUID id ? id : UUID.fromString(value.toString()); }
}

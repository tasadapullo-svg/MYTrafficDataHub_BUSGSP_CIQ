package com.mytransitgps.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/**
 * 中文名称：PostgreSQL JSONB 类型处理器。
 *
 * 功能说明：在 Jackson JsonNode 与 PostgreSQL jsonb 之间进行无损转换，
 * 保留对象、数组、数字、字符串和显式 null 的 JSON 类型语义。
 */
public class PostgreSqlJsonbTypeHandler extends BaseTypeHandler<JsonNode> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType) throws SQLException {
        PGobject value = new PGobject();
        value.setType("jsonb");
        value.setValue(parameter.toString());
        ps.setObject(i, value);
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getObject(columnName));
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getObject(columnIndex));
    }

    @Override
    public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getObject(columnIndex));
    }

    private JsonNode parse(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        String json = value instanceof PGobject pgObject ? pgObject.getValue() : value.toString();
        try {
            return json == null ? null : OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new SQLException("无法解析 PostgreSQL JSONB。", ex);
        }
    }
}

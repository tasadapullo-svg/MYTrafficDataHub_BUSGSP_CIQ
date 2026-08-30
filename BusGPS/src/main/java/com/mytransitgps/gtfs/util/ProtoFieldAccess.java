package com.mytransitgps.gtfs.util;

import java.util.Collections;
import java.util.List;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageOrBuilder;

/**
 * Protobuf 字段访问工具，安全读取可选字段并保留“是否存在”的语义。
 */
public final class ProtoFieldAccess {

    private ProtoFieldAccess() {
    }

    public static boolean hasField(MessageOrBuilder message, String fieldName) {
        Descriptors.FieldDescriptor field = findField(message, fieldName);
        return field != null && message.hasField(field);
    }

    public static Object getField(MessageOrBuilder message, String fieldName) {
        Descriptors.FieldDescriptor field = findField(message, fieldName);
        if (field == null || !message.hasField(field)) {
            return null;
        }
        return message.getField(field);
    }

    public static String getEnumName(MessageOrBuilder message, String fieldName) {
        Object value = getField(message, fieldName);
        if (value instanceof Descriptors.EnumValueDescriptor enumValue) {
            return enumValue.getName();
        }
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getRepeatedField(MessageOrBuilder message, String fieldName) {
        Descriptors.FieldDescriptor field = findField(message, fieldName);
        if (field == null) {
            return List.of();
        }
        Object value = message.getField(field);
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return Collections.emptyList();
    }

    private static Descriptors.FieldDescriptor findField(MessageOrBuilder message, String fieldName) {
        return message.getDescriptorForType().findFieldByName(fieldName);
    }
}

package com.mytransitgps.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 公共 SHA-256 哈希工具。
 *
 * <p>仅处理字节到十六进制摘要的底层能力，不包含 BUS GPS 或 CIQ 的业务路径、文件命名和归档规则。</p>
 */
public final class HashUtils {
    private HashUtils() {
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}


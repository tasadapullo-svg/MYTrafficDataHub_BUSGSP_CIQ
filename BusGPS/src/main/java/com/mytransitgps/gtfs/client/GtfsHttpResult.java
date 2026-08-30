package com.mytransitgps.gtfs.client;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 一次 GTFS HTTP 请求的不可变证据，包含时间、状态、响应体、重定向和异常信息。
 */
public record GtfsHttpResult(
        String requestType,
        URI requestedUri,
        URI finalUri,
        int statusCode,
        String contentType,
        long latencyMs,
        Instant requestStartedAt,
        Instant responseReceivedAt,
        Instant ingestTimestampUtc,
        byte[] responseBody,
        String responseSha256,
        int redirectCount,
        Map<String, List<String>> headers,
        String errorClass,
        String errorMessage) {

    public boolean isHttpOk() {
        return statusCode == 200;
    }

    public long responseBytes() {
        return responseBody == null ? 0L : responseBody.length;
    }

    public String firstHeader(String name) {
        List<String> values = headers.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
}

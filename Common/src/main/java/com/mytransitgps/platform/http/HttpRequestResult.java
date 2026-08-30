package com.mytransitgps.platform.http;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 平台HTTP请求结果，只保存协议层证据，不解释任何业务JSON字段。 */
public record HttpRequestResult(
        URI requestUri,
        URI responseUri,
        int statusCode,
        byte[] body,
        Map<String, List<String>> headers,
        Instant requestStartTime,
        Instant requestEndTime,
        long latencyMs,
        String errorCode,
        String errorMessage
) {
    public HttpRequestResult {
        body = body == null ? new byte[0] : body.clone();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public boolean successful() {
        return errorCode == null && statusCode >= 200 && statusCode < 300;
    }
}

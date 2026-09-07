package com.mytransitgps.modules.ciqbus.client;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record RawHttpResponse(
        URI requestUri,
        URI responseUri,
        int statusCode,
        byte[] body,
        Map<String, List<String>> headers,
        String errorCode,
        String errorMessage
) {
    public RawHttpResponse {
        body = body == null ? new byte[0] : body.clone();
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public String contentType() {
        return headers.entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .findFirst()
                .orElse("");
    }
}

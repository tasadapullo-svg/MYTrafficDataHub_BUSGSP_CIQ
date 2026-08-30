package com.mytransitgps.platform.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/** 平台HTTP访问端口，供CIQ及未来模块复用；不替换当前稳定的GTFS客户端。 */
public interface TrafficHttpClient {
    HttpRequestResult get(URI uri, Map<String, String> headers,
                          Duration connectTimeout, Duration requestTimeout);
}

package com.mytransitgps.platform.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 基于 JDK HttpClient 的平台 HTTP 适配器。
 *
 * <p>按连接超时配置复用 HttpClient，启用 JDK keep-alive；默认禁止自动重定向，避免携带
 * AccountKey 或 Authorization 的请求被转发到未验证 Host。</p>
 */
@Component
public class JdkTrafficHttpClient implements TrafficHttpClient {
    private final Map<Duration, HttpClient> clients = new ConcurrentHashMap<>();

    @Override
    public HttpRequestResult get(URI uri, Map<String, String> headers,
                                 Duration connectTimeout, Duration requestTimeout) {
        if (uri == null || !uri.isAbsolute()) {
            throw new HttpClientException("HTTP request URI must be absolute");
        }
        HttpClient client = clients.computeIfAbsent(connectTimeout, timeout -> HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .GET();
        headers.forEach(requestBuilder::header);

        Instant startTime = Instant.now();
        long startNanos = System.nanoTime();
        try {
            HttpResponse<byte[]> response = client.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            Instant endTime = Instant.now();
            return new HttpRequestResult(
                    uri, response.uri(), response.statusCode(), response.body(), response.headers().map(),
                    startTime, endTime, Duration.ofNanos(System.nanoTime() - startNanos).toMillis(), null, null);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new HttpClientException("HTTP request interrupted", error);
        } catch (IOException error) {
            Instant endTime = Instant.now();
            return new HttpRequestResult(
                    uri, uri, 0, new byte[0], Map.of(), startTime, endTime,
                    Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                    error.getClass().getSimpleName(), safeMessage(error));
        }
    }

    private static String safeMessage(IOException error) {
        return error.getMessage() == null ? "HTTP I/O failure" : error.getMessage();
    }
}


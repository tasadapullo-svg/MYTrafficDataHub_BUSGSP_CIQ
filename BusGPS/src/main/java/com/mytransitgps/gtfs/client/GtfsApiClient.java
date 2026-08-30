package com.mytransitgps.gtfs.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mytransitgps.gtfs.util.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GTFS 数据 HTTP 客户端，统一处理请求时间、重定向、响应字节和失败证据。
 */
public class GtfsApiClient {

    private static final Logger log = LoggerFactory.getLogger(GtfsApiClient.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_MIN_REQUEST_GAP = Duration.ofSeconds(25);
    private static final Duration DEFAULT_429_BACKOFF = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final Clock clock;
    private final Duration minRequestGap;
    private Instant nextRequestNotBefore;

    public GtfsApiClient() {
        this(Clock.systemUTC(), DEFAULT_MIN_REQUEST_GAP);
    }

    public GtfsApiClient(Clock clock, Duration minRequestGap) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.clock = clock;
        this.minRequestGap = minRequestGap;
    }

    public GtfsHttpResult fetch(String requestType, String url) {
        // 发送请求前统一执行预算等待，防止多个调用点绕过最小请求间隔。
        waitForBudget();

        // 在 HTTP 边界记录请求时刻，后续调度漂移和网络延迟均以此为准。
        Instant requestStartedAt = clock.instant();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "*/*")
                .header("User-Agent", "MYTransitGPS/0.1-demo")
                .GET()
                .build();

        long startNanos = System.nanoTime();
        try {
            log.info("GTFS HTTP接口调用开始，请求类型={}，host={}，path={}",
                    requestType, request.uri().getHost(), request.uri().getPath());
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            long latencyMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            Instant responseReceivedAt = clock.instant();
            Instant ingestTimestampUtc = clock.instant();
            byte[] responseBody = response.body() == null ? new byte[0] : response.body();
            String sha256 = response.statusCode() == 200 ? HashUtils.sha256Hex(responseBody) : null;
            Map<String, List<String>> headers = new LinkedHashMap<>(response.headers().map());

            // 429 优先采用服务端 Retry-After，其余响应恢复默认请求间隔。
            applyBackoff(response.statusCode(), response.headers());
            if (response.statusCode() == 200) {
                log.info("GTFS HTTP请求完成，请求类型={}，httpStatus={}，latencyMs={}，responseBytes={}，redirectCount={}，host={}，path={}",
                        requestType, response.statusCode(), latencyMs, responseBody.length, countRedirects(response),
                        request.uri().getHost(), request.uri().getPath());
            } else if (response.statusCode() == 429) {
                log.warn("GTFS HTTP请求触发限流，请求类型={}，httpStatus=429，latencyMs={}，retryAfter={}，host={}，path={}",
                        requestType, latencyMs, response.headers().firstValue("Retry-After").orElse("NOT_PROVIDED"),
                        request.uri().getHost(), request.uri().getPath());
            } else {
                log.warn("GTFS HTTP请求返回非200状态，请求类型={}，httpStatus={}，latencyMs={}，responseBytes={}，host={}，path={}",
                        requestType, response.statusCode(), latencyMs, responseBody.length, request.uri().getHost(), request.uri().getPath());
            }

            return new GtfsHttpResult(
                    requestType,
                    request.uri(),
                    response.uri(),
                    response.statusCode(),
                    response.headers().firstValue("Content-Type").orElse(null),
                    latencyMs,
                    requestStartedAt,
                    responseReceivedAt,
                    ingestTimestampUtc,
                    responseBody,
                    sha256,
                    countRedirects(response),
                    headers,
                    null,
                    null);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException interruptedException) {
                // 恢复中断标志，让上层生命周期能够识别正常 Stop，而不是继续采集。
                Thread.currentThread().interrupt();
                log.info("GTFS HTTP请求收到线程中断信号，请求类型={}，host={}，path={}",
                        requestType, request.uri().getHost(), request.uri().getPath());
            } else {
                log.error("GTFS HTTP请求执行失败，请求类型={}，host={}，path={}，异常类型={}，错误信息={}",
                        requestType, request.uri().getHost(), request.uri().getPath(), ex.getClass().getSimpleName(), ex.getMessage());
            }

            Instant failedAt = clock.instant();
            nextRequestNotBefore = failedAt.plus(minRequestGap);
            return new GtfsHttpResult(
                    requestType,
                    request.uri(),
                    request.uri(),
                    0,
                    null,
                    Duration.ofNanos(System.nanoTime() - startNanos).toMillis(),
                    requestStartedAt,
                    failedAt,
                    failedAt,
                    new byte[0],
                    null,
                    0,
                    Map.of(),
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
        }
    }

    private void waitForBudget() {
        Instant now = clock.instant();
        if (nextRequestNotBefore != null && now.isBefore(nextRequestNotBefore)) {
            Duration sleepDuration = Duration.between(now, nextRequestNotBefore);
            try {
                Thread.sleep(sleepDuration.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.info("GTFS请求预算等待收到线程中断信号，等待毫秒={}", sleepDuration.toMillis());
                throw new IllegalStateException("Interrupted while enforcing GTFS request budget.", ex);
            }
        }
    }

    private void applyBackoff(int statusCode, HttpHeaders headers) {
        Instant now = clock.instant();
        if (statusCode == 429) {
            Duration retryAfter = parseRetryAfter(headers.firstValue("Retry-After").orElse(null));
            nextRequestNotBefore = now.plus(retryAfter == null ? DEFAULT_429_BACKOFF : retryAfter);
            return;
        }
        nextRequestNotBefore = now.plus(minRequestGap);
    }

    private Duration parseRetryAfter(String retryAfterValue) {
        if (retryAfterValue == null || retryAfterValue.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(retryAfterValue.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int countRedirects(HttpResponse<?> response) {
        int count = 0;
        HttpResponse<?> current = response;
        while (current.previousResponse().isPresent()) {
            count++;
            current = current.previousResponse().get();
        }
        return count;
    }
}

package com.mytransitgps.modules.ciq.client;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LTA TrafficSpeedBands协议客户端骨架。
 * 只负责HTTP证据，不解析业务字段、不持久化，也不记录AccountKey。
 */
public class LtaTrafficSpeedClient {
    private static final Logger log = LoggerFactory.getLogger(LtaTrafficSpeedClient.class);
    private final TrafficHttpClient httpClient;
    private final CiqProperties properties;
    private final RetrySleeper sleeper;

    public LtaTrafficSpeedClient(TrafficHttpClient httpClient, CiqProperties properties) {
        this(httpClient, properties, duration -> Thread.sleep(duration.toMillis()));
    }

    LtaTrafficSpeedClient(TrafficHttpClient httpClient, CiqProperties properties, RetrySleeper sleeper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.sleeper = sleeper;
        if (!properties.isAccountKeyConfigured()) {
            log.warn("LTA AccountKey未配置；CIQ客户端将拒绝发送HTTP请求");
        }
    }

    public URI firstPageUri() {
        requireConfigured();
        return URI.create(join(properties.getLta().getBaseUrl(),
                properties.getCollectors().getTrafficSpeed().getEndpoint()));
    }

    public LtaHttpResponse fetchRaw() {
        return fetchPage(firstPageUri());
    }

    public LtaHttpResponse fetchPage(URI uri) {
        requireConfigured();
        requireAllowedUri(uri);
        int maximumRetries = Math.max(0, properties.getHttp().getRetryCount());
        int retries = 0;
        while (true) {
            log.info("CIQ API01 [HTTP-N01] 请求发送，uri={}，attempt={}", uri, retries + 1);
            HttpRequestResult result = httpClient.get(uri,
                    Map.of("AccountKey", properties.getLta().getAccountKey(),
                            "Accept", properties.getLta().getAccept()),
                    Duration.ofSeconds(properties.getHttp().getConnectTimeoutSeconds()),
                    Duration.ofSeconds(properties.getHttp().getRequestTimeoutSeconds()));
            log.info("CIQ API01 [HTTP-N02] 响应返回，status={}，bytes={}，latencyMs={}，errorCode={}", result.statusCode(), result.body().length, result.latencyMs(), result.errorCode());
            if (!retryable(result) || retries >= maximumRetries) {
                return new LtaHttpResponse(result, retries);
            }
            retries++;
            Duration delay = retryDelay(result);
            log.warn("CIQ接口请求失败准备重试，collector=CIQ_TRAFFIC_SPEED，attempt={}/{}，status={}，delaySeconds={}",
                    retries, maximumRetries, result.statusCode(), delay.toSeconds());
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("CIQ重试等待被中断", error);
            }
        }
    }

    private void requireConfigured() {
        if (!properties.isAccountKeyConfigured()) {
            throw new IllegalStateException("LTA_ACCOUNT_KEY未配置，已禁止发送请求");
        }
    }

    private void requireAllowedUri(URI uri) {
        URI base = URI.create(properties.getLta().getBaseUrl());
        if (uri == null || !uri.isAbsolute() || !same(base.getScheme(), uri.getScheme())
                || !same(base.getHost(), uri.getHost())) {
            throw new IllegalArgumentException("LTA分页地址不属于已配置的数据源Host");
        }
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static boolean retryable(HttpRequestResult result) {
        int status = result.statusCode();
        return status == 0 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private Duration retryDelay(HttpRequestResult result) {
        if (result.statusCode() == 429) {
            Duration retryAfter = parseRetryAfter(result.headers());
            if (retryAfter != null) return retryAfter;
        }
        return Duration.ofSeconds(Math.max(0, properties.getHttp().getRetryDelaySeconds()));
    }

    private static Duration parseRetryAfter(Map<String, List<String>> headers) {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("Retry-After") || entry.getValue().isEmpty()) continue;
            String value = entry.getValue().get(0).trim();
            try {
                return Duration.ofSeconds(Math.max(0, Long.parseLong(value)));
            } catch (NumberFormatException ignored) {
                try {
                    Duration duration = Duration.between(ZonedDateTime.now(),
                            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME));
                    return duration.isNegative() ? Duration.ZERO : duration;
                } catch (RuntimeException invalidDate) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String join(String baseUrl, String endpoint) {
        return baseUrl.endsWith("/") && endpoint.startsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1) + endpoint
                : baseUrl + endpoint;
    }

    /** 为测试隔离重试等待行为的函数接口。 */
    @FunctionalInterface
    interface RetrySleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}

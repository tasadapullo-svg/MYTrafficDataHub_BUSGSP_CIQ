package com.mytransitgps.modules.ciqbus.client;

import com.mytransitgps.modules.ciqbus.config.CiqBusProperties;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LtaCrossBorderBusClient {
    private static final Logger log = LoggerFactory.getLogger(LtaCrossBorderBusClient.class);

    private final TrafficHttpClient httpClient;
    private final CiqBusProperties properties;

    public LtaCrossBorderBusClient(TrafficHttpClient httpClient, CiqBusProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public RawHttpResponse fetchBusArrival(String stopCode) {
        if (!properties.isLtaAccountKeyConfigured()) {
            throw new IllegalStateException("LTA_ACCOUNT_KEY 未配置，已阻止 LTA 请求发送");
        }
        URI uri = URI.create(join(properties.getLta().getBaseUrl(), "/v3/BusArrival") + "?BusStopCode=" + stopCode);
        log.info("[CIQBUS-LTA] 项目进度：开始请求新加坡LTA跨境巴士到站数据，stopCode={}，url={}", stopCode, uri);
        URI base = URI.create(properties.getLta().getBaseUrl());
        if (!same(base.getScheme(), uri.getScheme()) || !same(base.getHost(), uri.getHost())) {
            throw new IllegalArgumentException("LTA BusArrival 地址不属于已配置数据源 Host");
        }
        HttpRequestResult result = httpClient.get(uri,
                Map.of("AccountKey", properties.getLta().getAccountKey(), "Accept", properties.getLta().getAccept()),
                Duration.ofSeconds(properties.getHttp().getConnectTimeoutSeconds()),
                Duration.ofSeconds(properties.getHttp().getRequestTimeoutSeconds()));
        log.info("[CIQBUS-LTA] 项目进度：LTA请求完成，stopCode={}，httpStatus={}，响应字节={}，errorCode={}",
                stopCode, result.statusCode(), result.body() == null ? 0 : result.body().length, result.errorCode());
        return new RawHttpResponse(result.requestUri(), result.responseUri(), result.statusCode(), result.body(),
                result.headers(), result.errorCode(), result.errorMessage());
    }

    private static String join(String base, String endpoint) {
        if (base.endsWith("/") && endpoint.startsWith("/")) {
            return base.substring(0, base.length() - 1) + endpoint;
        }
        if (!base.endsWith("/") && !endpoint.startsWith("/")) {
            return base + "/" + endpoint;
        }
        return base + endpoint;
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}

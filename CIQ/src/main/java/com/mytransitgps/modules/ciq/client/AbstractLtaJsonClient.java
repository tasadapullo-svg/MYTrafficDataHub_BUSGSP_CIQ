package com.mytransitgps.modules.ciq.client;

import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CIQ JSON API 共用 HTTP 客户端：认证、超时、有限重试、Host 白名单和节点日志。 */
public abstract class AbstractLtaJsonClient {
    private static final Logger log = LoggerFactory.getLogger(AbstractLtaJsonClient.class);
    protected final TrafficHttpClient httpClient; protected final CiqProperties properties;
    private final String apiCode; private final CiqProperties.CollectorSettings settings;
    protected AbstractLtaJsonClient(TrafficHttpClient httpClient, CiqProperties properties, String apiCode, CiqProperties.CollectorSettings settings){
        this.httpClient=httpClient;this.properties=properties;this.apiCode=apiCode;this.settings=settings;
    }
    public URI firstPageUri(){ requireConfigured(); return URI.create(join(properties.getLta().getBaseUrl(), settings.getEndpoint())); }
    public LtaHttpResponse fetchPage(URI uri){
        requireConfigured(); requireAllowedLtaUri(uri);
        int retries=0,max=Math.max(0,properties.getHttp().getRetryCount());
        while(true){
            log.info("CIQ {} [HTTP-N01] 请求发送，uri={}，attempt={}", apiCode, uri, retries+1);
            HttpRequestResult r=httpClient.get(uri, Map.of("AccountKey",properties.getLta().getAccountKey(),"Accept",properties.getLta().getAccept()),
                    Duration.ofSeconds(properties.getHttp().getConnectTimeoutSeconds()),Duration.ofSeconds(properties.getHttp().getRequestTimeoutSeconds()));
            log.info("CIQ {} [HTTP-N02] 响应返回，status={}，bytes={}，latencyMs={}，errorCode={}",apiCode,r.statusCode(),r.body().length,r.latencyMs(),r.errorCode());
            if(!retryable(r)||retries>=max) return new LtaHttpResponse(r,retries);
            retries++;
            log.warn("CIQ {} [HTTP-N03] 可重试失败，retry={}/{}，status={}，delaySeconds={}",apiCode,retries,max,r.statusCode(),properties.getHttp().getRetryDelaySeconds());
            try{Thread.sleep(Math.max(0,properties.getHttp().getRetryDelaySeconds())*1000L);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("CIQ重试等待被中断",e);}
        }
    }
    protected void requireConfigured(){ if(!properties.isAccountKeyConfigured()) throw new IllegalStateException("LTA_ACCOUNT_KEY未配置，已禁止发送请求"); }
    protected void requireAllowedLtaUri(URI uri){
        URI base=URI.create(properties.getLta().getBaseUrl());
        if(uri==null||!uri.isAbsolute()||!same(base.getScheme(),uri.getScheme())||!same(base.getHost(),uri.getHost())) throw new IllegalArgumentException("LTA分页地址不属于已配置的数据源Host");
    }
    private static boolean retryable(HttpRequestResult r){int s=r.statusCode();return s==0||s==429||s==500||s==502||s==503||s==504;}
    private static boolean same(String a,String b){return a!=null&&b!=null&&a.equalsIgnoreCase(b);}
    private static String join(String b,String e){return b.endsWith("/")&&e.startsWith("/")?b.substring(0,b.length()-1)+e:b+e;}
}

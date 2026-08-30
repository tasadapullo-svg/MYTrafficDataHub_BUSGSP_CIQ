package com.mytransitgps.platform.http;

/** 平台HTTP配置或执行异常，不包含认证Header等敏感数据。 */
public class HttpClientException extends RuntimeException {
    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

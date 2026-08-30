package com.mytransitgps.modules.ciq.client;

import com.mytransitgps.platform.http.HttpRequestResult;

/** 单页LTA请求结果以及实际发生的重试次数。 */
public record LtaHttpResponse(HttpRequestResult http, int retryCount) {
}

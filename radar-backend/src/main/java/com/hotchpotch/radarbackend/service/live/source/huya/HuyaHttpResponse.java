package com.hotchpotch.radarbackend.service.live.source.huya;

/**
 * 虎牙页面 HTTP 响应的最小统一表示。
 */
final class HuyaHttpResponse {

    /**
     * HTTP 状态码。
     */
    private final int statusCode;

    /**
     * HTTP 重定向地址。
     */
    private final String location;

    /**
     * 页面响应正文。
     */
    private final String body;

    /**
     * 创建虎牙页面 HTTP 响应。
     *
     * @param statusCode HTTP 状态码
     * @param location HTTP 重定向地址
     * @param body 页面响应正文
     */
    HuyaHttpResponse(int statusCode, String location, String body) {
        this.statusCode = statusCode;
        this.location = location;
        this.body = body;
    }

    int getStatusCode() {
        return statusCode;
    }

    String getLocation() {
        return location;
    }

    String getBody() {
        return body;
    }
}

package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 直播间 URL 安全解析配置。
 */
@ConfigurationProperties(prefix = "radar.url")
public class RadarUrlProperties {

    /**
     * 单个 URL 允许的最大字符数。
     */
    private int maxLength = 2048;

    /**
     * 短链接允许的最大重定向次数。
     */
    private int maxRedirects = 3;

    /**
     * 建立短链接连接的超时时间，单位为毫秒。
     */
    private int connectTimeoutMs = 3000;

    /**
     * 短链接请求响应超时时间，单位为毫秒。
     */
    private int responseTimeoutMs = 5000;

    /**
     * 短链接请求使用的固定 User-Agent。
     */
    private String userAgent = "StreamRadar/1.0";

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxRedirects() {
        return maxRedirects;
    }

    public void setMaxRedirects(int maxRedirects) {
        this.maxRedirects = maxRedirects;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getResponseTimeoutMs() {
        return responseTimeoutMs;
    }

    public void setResponseTimeoutMs(int responseTimeoutMs) {
        this.responseTimeoutMs = responseTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}

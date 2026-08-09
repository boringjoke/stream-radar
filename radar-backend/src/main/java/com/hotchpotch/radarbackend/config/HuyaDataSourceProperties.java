package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 虎牙数据源请求配置。
 */
@ConfigurationProperties(prefix = "radar.huya")
public class HuyaDataSourceProperties {

    /**
     * 虎牙直播间网页根地址。
     */
    private String webBaseUrl = "https://www.huya.com";

    /**
     * 建立数据源连接的超时时间，单位为毫秒。
     */
    private int connectTimeoutMs = 5000;

    /**
     * 数据源响应超时时间，单位为毫秒。
     */
    private int responseTimeoutMs = 10000;

    /**
     * 数据源读取超时时间，单位为毫秒。
     */
    private int readTimeoutMs = 10000;

    /**
     * 数据源写入超时时间，单位为毫秒。
     */
    private int writeTimeoutMs = 10000;

    /**
     * 数据源请求使用的 User-Agent。
     */
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 "
            + "Safari/537.36 StreamRadar/1.0";

    /**
     * WebClient 单次允许缓存的响应大小，单位为字节。
     */
    private int maxInMemorySizeBytes = 4 * 1024 * 1024;

    public String getWebBaseUrl() {
        return webBaseUrl;
    }

    public void setWebBaseUrl(String webBaseUrl) {
        this.webBaseUrl = webBaseUrl;
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

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getWriteTimeoutMs() {
        return writeTimeoutMs;
    }

    public void setWriteTimeoutMs(int writeTimeoutMs) {
        this.writeTimeoutMs = writeTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getMaxInMemorySizeBytes() {
        return maxInMemorySizeBytes;
    }

    public void setMaxInMemorySizeBytes(int maxInMemorySizeBytes) {
        this.maxInMemorySizeBytes = maxInMemorySizeBytes;
    }
}

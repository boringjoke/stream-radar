package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 直播 SSE 连接配置。
 */
@ConfigurationProperties(prefix = "radar.sse")
public class LiveSseProperties {

    /**
     * SSE 发射器超时时间，单位毫秒；0 表示由心跳维持长连接。
     */
    private long emitterTimeoutMs = 0L;

    public long getEmitterTimeoutMs() {
        return emitterTimeoutMs;
    }

    public void setEmitterTimeoutMs(long emitterTimeoutMs) {
        this.emitterTimeoutMs = emitterTimeoutMs;
    }
}

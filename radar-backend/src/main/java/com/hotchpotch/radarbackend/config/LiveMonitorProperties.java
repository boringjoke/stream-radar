package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 直播监控持久化配置。
 */
@ConfigurationProperties(prefix = "radar.monitor")
public class LiveMonitorProperties {

    /**
     * 监控健康信息的最小持久化间隔，单位毫秒。
     */
    private long healthPersistIntervalMs = 300_000L;

    /**
     * 数据源错误摘要的最大保存长度。
     */
    private int maxErrorMessageLength = 500;

    public long getHealthPersistIntervalMs() {
        return healthPersistIntervalMs;
    }

    public void setHealthPersistIntervalMs(long healthPersistIntervalMs) {
        this.healthPersistIntervalMs = healthPersistIntervalMs;
    }

    public int getMaxErrorMessageLength() {
        return maxErrorMessageLength;
    }

    public void setMaxErrorMessageLength(int maxErrorMessageLength) {
        this.maxErrorMessageLength = maxErrorMessageLength;
    }
}

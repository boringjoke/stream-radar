package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 直播监控和 SSE 心跳的进程内调度配置。
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({LiveMonitorProperties.class, LiveSseProperties.class})
public class LiveSchedulingConfig {
}

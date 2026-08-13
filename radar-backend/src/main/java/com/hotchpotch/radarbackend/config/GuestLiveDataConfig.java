package com.hotchpotch.radarbackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 游客首页真实数据配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GuestLiveDemoProperties.class)
public class GuestLiveDataConfig {
}

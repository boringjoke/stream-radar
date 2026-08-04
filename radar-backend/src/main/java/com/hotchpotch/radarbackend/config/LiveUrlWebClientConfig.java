package com.hotchpotch.radarbackend.config;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 直播间短链接解析使用的 HTTP 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RadarUrlProperties.class)
public class LiveUrlWebClientConfig {

    /**
     * 创建不自动跟随重定向的短链接 HTTP 客户端。
     *
     * @param properties URL 解析配置
     * @return URL 解析专用 WebClient
     */
    @Bean
    public WebClient liveUrlWebClient(RadarUrlProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(properties.getResponseTimeoutMs()));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .build();
    }
}

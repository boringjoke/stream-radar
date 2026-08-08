package com.hotchpotch.radarbackend.config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 斗鱼数据源 HTTP 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DouyuDataSourceProperties.class)
public class DouyuDataSourceConfig {

    /**
     * 创建斗鱼数据源专用 WebClient。
     *
     * @param properties 斗鱼数据源配置
     * @return 斗鱼数据源 HTTP 客户端
     */
    @Bean(name = "douyuWebClient")
    public WebClient douyuWebClient(DouyuDataSourceProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                // 斗鱼直播中的网页响应可能返回 gzip，必须由 Reactor Netty 自动解压后再交给 HTML Parser。
                .compress(true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Math.max(1000, properties.getConnectTimeoutMs()))
                .responseTimeout(Duration.ofMillis(Math.max(1000, properties.getResponseTimeoutMs())))
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(
                                Math.max(1000, properties.getReadTimeoutMs()), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                Math.max(1000, properties.getWriteTimeoutMs()), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(Math.max(1024 * 1024, properties.getMaxInMemorySizeBytes())))
                .build();
    }
}

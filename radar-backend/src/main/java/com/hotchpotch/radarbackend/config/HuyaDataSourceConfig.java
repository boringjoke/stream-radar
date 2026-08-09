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
 * 虎牙数据源 HTTP 客户端配置。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HuyaDataSourceProperties.class)
public class HuyaDataSourceConfig {

    /**
     * 创建虎牙数据源专用 WebClient。
     *
     * @param properties 虎牙数据源配置
     * @return 虎牙数据源 HTTP 客户端
     */
    @Bean(name = "huyaWebClient")
    public WebClient huyaWebClient(HuyaDataSourceProperties properties) {
        HttpClient httpClient = HttpClient.create()
                // 不自动跟随重定向，由 Provider 读取 Location 中的结构化错误类型。
                .followRedirect(false)
                // 虎牙页面可能返回压缩内容，先由 Reactor Netty 自动解压。
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
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(Math.max(1024 * 1024, properties.getMaxInMemorySizeBytes())))
                .build();
    }
}

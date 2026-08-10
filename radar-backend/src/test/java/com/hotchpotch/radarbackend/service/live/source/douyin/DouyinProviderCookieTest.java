package com.hotchpotch.radarbackend.service.live.source.douyin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import com.hotchpotch.radarbackend.config.DouyinDataSourceConfig;
import com.hotchpotch.radarbackend.config.DouyinDataSourceProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 抖音主数据源页面 Cookie 和接口调用定向验证。
 */
class DouyinProviderCookieTest {

    private final AtomicBoolean apiReceivedTtwid = new AtomicBoolean();

    private DisposableServer server;

    private DouyinApiProvider apiProvider;

    @BeforeEach
    void setUp() {
        server = HttpServer.create()
                .port(0)
                .handle((request, response) -> {
                    if (request.uri().startsWith("/998")) {
                        return response
                                .status(HttpResponseStatus.OK)
                                .header(HttpHeaderNames.SET_COOKIE, "ttwid=test-value; Path=/")
                                .sendString(Mono.just("<html><body>room page</body></html>"));
                    }
                    if (request.uri().startsWith("/webcast/room/web/enter/")) {
                        String cookie = request.requestHeaders().get(HttpHeaderNames.COOKIE);
                        apiReceivedTtwid.set(cookie != null && cookie.contains("ttwid=test-value"));
                        return response
                                .status(HttpResponseStatus.OK)
                                .header(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8")
                                .sendString(Mono.just(
                                        "{\"status_code\":0,\"data\":{\"room_status\":0,"
                                                + "\"user\":{\"id_str\":\"1001\",\"nickname\":\"主播\"},"
                                                + "\"data\":[{\"id_str\":\"internal\",\"status\":2,"
                                                + "\"title\":\"title\",\"user_count_str\":\"12\"}]}}"));
                    }
                    return response.status(HttpResponseStatus.NOT_FOUND).send();
                })
                .bindNow();

        DouyinDataSourceProperties properties = new DouyinDataSourceProperties();
        String baseUrl = "http://127.0.0.1:" + server.port();
        properties.setApiBaseUrl(baseUrl);
        properties.setWebBaseUrl(baseUrl);
        WebClient webClient = new DouyinDataSourceConfig().douyinWebClient(properties);
        apiProvider = new DouyinApiProvider(
                webClient,
                properties,
                new DouyinApiParser(new ObjectMapper()));
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldFetchTtwidBeforeCallingApi() {
        ResolvedLiveRoom room = new ResolvedLiveRoom(
                LivePlatform.DOUYIN,
                "998",
                "http://127.0.0.1/998");

        var result = apiProvider.resolve(room);

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
        assertEquals("title", result.getSnapshot().getLiveTitle());
        assertTrue(apiReceivedTtwid.get());
    }
}

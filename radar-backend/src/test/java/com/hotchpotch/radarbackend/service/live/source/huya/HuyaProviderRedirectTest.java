package com.hotchpotch.radarbackend.service.live.source.huya;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hotchpotch.radarbackend.config.HuyaDataSourceConfig;
import com.hotchpotch.radarbackend.config.HuyaDataSourceProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import tools.jackson.databind.ObjectMapper;

/**
 * 虎牙 Provider 结构化重定向定向验证。
 */
class HuyaProviderRedirectTest {

    private DisposableServer server;

    private HuyaPageProvider pageProvider;

    private HuyaDomProvider domProvider;

    @BeforeEach
    void setUp() {
        server = HttpServer.create()
                .port(0)
                .handle((request, response) -> {
                    if (request.uri().startsWith("/error")) {
                        return response.status(HttpResponseStatus.OK).send();
                    }
                    return response
                            .status(HttpResponseStatus.FOUND)
                            .header(HttpHeaderNames.LOCATION, "/error?errorType=ROOM_NOT_FOUND")
                            .send();
                })
                .bindNow();

        HuyaDataSourceProperties properties = new HuyaDataSourceProperties();
        properties.setWebBaseUrl("http://127.0.0.1:" + server.port());
        WebClient webClient = new HuyaDataSourceConfig().huyaWebClient(properties);
        pageProvider = new HuyaPageProvider(
                webClient,
                properties,
                new HuyaPageParser(new ObjectMapper()));
        domProvider = new HuyaDomProvider(
                webClient,
                properties,
                new HuyaDomParser());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.disposeNow();
        }
    }

    @Test
    void shouldConvertRoomNotFoundRedirectToNotFoundForBothProviders() {
        ResolvedLiveRoom room = new ResolvedLiveRoom(
                LivePlatform.HUYA,
                "9989999999",
                "http://127.0.0.1/9989999999");

        assertEquals(LiveSourceStatus.NOT_FOUND, pageProvider.resolve(room).getStatus());
        assertEquals(LiveSourceStatus.NOT_FOUND, domProvider.resolve(room).getStatus());
    }

}

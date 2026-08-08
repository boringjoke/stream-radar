package com.hotchpotch.radarbackend.service.live.source.bilibili;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.config.BilibiliDataSourceProperties;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * B站直播间网页备用数据源 Provider。
 */
@Component
public class BilibiliWebProvider {

    /**
     * B站数据源 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * B站数据源配置。
     */
    private final BilibiliDataSourceProperties properties;

    /**
     * B站网页解析器。
     */
    private final BilibiliWebParser parser;

    /**
     * 创建B站网页 Provider。
     *
     * @param webClient B站数据源 HTTP 客户端
     * @param properties B站数据源配置
     * @param parser B站网页解析器
     */
    public BilibiliWebProvider(
            @Qualifier("bilibiliWebClient") WebClient webClient,
            BilibiliDataSourceProperties properties,
            BilibiliWebParser parser) {
        this.webClient = webClient;
        this.properties = properties;
        this.parser = parser;
    }

    /**
     * 查询单个直播间网页。
     *
     * @param room URL 解析后的直播间身份
     * @return 备用数据源结果
     */
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        String roomId = room.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return LiveSourceResult.unknown("B站网页房间标识为空");
        }
        try {
            String responseBody = webClient.get()
                    .uri(URI.create(buildRoomUrl(roomId)))
                    .accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout());
            return parser.parse(responseBody, roomId);
        } catch (WebClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 404 || status == 410) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.temporarilyUnavailable("B站直播间网页 HTTP 请求失败");
        } catch (WebClientRequestException exception) {
            return LiveSourceResult.temporarilyUnavailable("B站直播间网页网络请求失败");
        } catch (RuntimeException exception) {
            return LiveSourceResult.temporarilyUnavailable("B站直播间网页请求失败");
        }
    }

    /**
     * 逐个查询主播网页状态，供监控路由复用。
     *
     * @param anchors 待查询主播列表
     * @return 与输入主播顺序对应的结果
     */
    public List<LiveSourceResult> queryStatus(List<LiveAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }
        List<LiveSourceResult> results = new ArrayList<>(anchors.size());
        for (LiveAnchor anchor : anchors) {
            if (anchor == null || anchor.getRoomId() == null || anchor.getRoomId().isBlank()) {
                results.add(LiveSourceResult.unknown("B站网页监控对象无效"));
                continue;
            }
            ResolvedLiveRoom room = new ResolvedLiveRoom(
                    LivePlatform.BILIBILI,
                    anchor.getRoomId(),
                    anchor.getRoomUrl());
            results.add(resolve(room));
        }
        return results;
    }

    /**
     * 创建网页请求地址。
     *
     * @param roomId 房间标识
     * @return 网页地址
     */
    private String buildRoomUrl(String roomId) {
        return trimTrailingSlash(properties.getWebBaseUrl()) + "/" + roomId;
    }

    /**
     * 创建请求超时时间。
     *
     * @return 请求超时时间
     */
    private Duration timeout() {
        return Duration.ofMillis(Math.max(1000, properties.getResponseTimeoutMs() + 1000L));
    }

    /**
     * 去除地址末尾斜杠。
     *
     * @param value 地址
     * @return 规范化地址
     */
    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}

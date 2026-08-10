package com.hotchpotch.radarbackend.service.live.source.douyin;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.config.DouyinDataSourceProperties;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 抖音直播页面备用数据源 Provider。
 */
@Component
public class DouyinWebProvider {

    /**
     * 抖音数据源 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 抖音数据源配置。
     */
    private final DouyinDataSourceProperties properties;

    /**
     * 抖音页面解析器。
     */
    private final DouyinWebParser parser;

    /**
     * 创建抖音页面备用 Provider。
     *
     * @param webClient 抖音数据源 HTTP 客户端
     * @param properties 抖音数据源配置
     * @param parser 抖音页面解析器
     */
    public DouyinWebProvider(
            @Qualifier("douyinWebClient") WebClient webClient,
            DouyinDataSourceProperties properties,
            DouyinWebParser parser) {
        this.webClient = webClient;
        this.properties = properties;
        this.parser = parser;
    }

    /**
     * 查询单个抖音直播页面。
     *
     * @param room URL 解析后的直播间身份
     * @return 页面备用数据源结果
     */
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        if (room == null || isBlank(room.getRoomId())) {
            return LiveSourceResult.unknown("抖音页面备用数据源房间标识为空");
        }

        try {
            String responseBody = webClient.get()
                    .uri(URI.create(buildRoomUrl(room.getRoomId())))
                    .headers(headers -> {
                        headers.set(HttpHeaders.ACCEPT,
                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
                        headers.set(HttpHeaders.REFERER, trimTrailingSlash(properties.getWebBaseUrl()) + "/");
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .block(timeout());
            return parser.parse(responseBody, room.getRoomId());
        } catch (WebClientResponseException exception) {
            if (isNotFoundHttpStatus(exception.getStatusCode().value())) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.temporarilyUnavailable("抖音直播页面 HTTP 请求失败");
        } catch (WebClientRequestException exception) {
            return LiveSourceResult.temporarilyUnavailable("抖音直播页面网络请求失败");
        } catch (RuntimeException exception) {
            return LiveSourceResult.temporarilyUnavailable("抖音直播页面请求失败");
        }
    }

    /**
     * 按主播列表逐个查询页面状态。
     *
     * @param anchors 待监控主播列表
     * @return 与输入主播顺序对应的结果
     */
    public List<LiveSourceResult> queryStatus(List<LiveAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }

        List<LiveSourceResult> results = new ArrayList<>(anchors.size());
        for (LiveAnchor anchor : anchors) {
            if (anchor == null || isBlank(anchor.getRoomId())) {
                results.add(LiveSourceResult.unknown("抖音页面监控对象无效"));
                continue;
            }
            ResolvedLiveRoom room = new ResolvedLiveRoom(
                    LivePlatform.DOUYIN,
                    anchor.getRoomId(),
                    anchor.getRoomUrl());
            results.add(mergeWithExistingAnchor(resolve(room), anchor));
        }
        return results;
    }

    /**
     * 将页面快照与已有主播资料合并。
     *
     * @param result 页面数据源结果
     * @param anchor 已保存主播
     * @return 合并后的结果
     */
    private LiveSourceResult mergeWithExistingAnchor(LiveSourceResult result, LiveAnchor anchor) {
        if (result == null || !result.isAvailable()) {
            return result;
        }
        LiveSnapshot snapshot = result.getSnapshot();
        LiveSnapshot merged = new LiveSnapshot(
                LivePlatform.DOUYIN,
                firstNonBlank(snapshot.getRoomId(), anchor.getRoomId()),
                firstNonBlank(snapshot.getPlatformUid(), anchor.getPlatformUid()),
                firstNonBlank(snapshot.getAnchorName(), anchor.getAnchorName()),
                firstNonBlank(snapshot.getAvatarUrl(), anchor.getAvatarUrl()),
                firstNonBlank(snapshot.getCoverUrl(), anchor.getCoverUrl()),
                firstNonBlank(snapshot.getLiveTitle(), anchor.getLiveTitle()),
                snapshot.getOnlineCount() == null ? anchor.getOnlineCount() : snapshot.getOnlineCount(),
                snapshot.getLiveStatus());
        return LiveSourceResult.available(merged);
    }

    /**
     * 创建抖音直播页地址。
     *
     * @param roomId 网页房间标识
     * @return 直播页地址
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
     * 判断 HTTP 状态是否明确表示房间不存在。
     *
     * @param status HTTP 状态码
     * @return 是否为明确不存在
     */
    private boolean isNotFoundHttpStatus(int status) {
        return status == 404 || status == 410;
    }

    /**
     * 选择第一个非空文本。
     *
     * @param first 优先值
     * @param fallback 备用值
     * @return 非空值
     */
    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    /**
     * 去除根地址末尾斜杠。
     *
     * @param value 原始地址
     * @return 清理后的地址
     */
    private String trimTrailingSlash(String value) {
        if (isBlank(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

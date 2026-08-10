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
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 抖音直播 JSON 主数据源 Provider。
 */
@Component
public class DouyinApiProvider {

    /**
     * 抖音直播 JSON 接口路径。
     */
    private static final String API_PATH = "/webcast/room/web/enter/";

    /**
     * 抖音数据源 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 抖音数据源配置。
     */
    private final DouyinDataSourceProperties properties;

    /**
     * 抖音主数据源解析器。
     */
    private final DouyinApiParser parser;

    /**
     * 创建抖音主数据源 Provider。
     *
     * @param webClient 抖音数据源 HTTP 客户端
     * @param properties 抖音数据源配置
     * @param parser 抖音主数据源解析器
     */
    public DouyinApiProvider(
            @Qualifier("douyinWebClient") WebClient webClient,
            DouyinDataSourceProperties properties,
            DouyinApiParser parser) {
        this.webClient = webClient;
        this.properties = properties;
        this.parser = parser;
    }

    /**
     * 查询单个抖音直播间资料和状态。
     *
     * <p>抖音当前接口在没有 Cookie 时可能返回空正文，因此每次主接口请求前先访问对应直播页，
     * 从响应 Cookie 中取得 ttwid，再调用 JSON 接口。</p>
     *
     * @param room URL 解析后的直播间身份
     * @return 主数据源结果
     */
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        if (room == null || isBlank(room.getRoomId())) {
            return LiveSourceResult.unknown("抖音主数据源房间标识为空");
        }

        try {
            PageResponse pageResponse = requestRoomPage(room.getRoomId());
            if (pageResponse == null) {
                return LiveSourceResult.temporarilyUnavailable("抖音主数据源未返回直播页响应");
            }
            if (isNotFoundHttpStatus(pageResponse.statusCode())) {
                return LiveSourceResult.notFound();
            }
            if (pageResponse.statusCode() >= 400) {
                return LiveSourceResult.temporarilyUnavailable("抖音主数据源直播页请求失败");
            }

            ResponseCookie ttwid = pageResponse.ttwid();
            if (ttwid == null || isBlank(ttwid.getValue())) {
                return LiveSourceResult.temporarilyUnavailable("抖音主数据源未取得 ttwid");
            }

            String responseBody = webClient.get()
                    .uri(buildApiUri(room.getRoomId()))
                    .headers(headers -> {
                        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                        headers.set(HttpHeaders.REFERER, trimTrailingSlash(properties.getWebBaseUrl()) + "/");
                        headers.set(HttpHeaders.COOKIE, "ttwid=" + ttwid.getValue());
                    })
                    .retrieve()
                    .bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .block(timeout());
            if (isBlank(responseBody)) {
                return LiveSourceResult.temporarilyUnavailable("抖音主数据源接口响应为空");
            }
            return parser.parse(responseBody, room.getRoomId());
        } catch (WebClientResponseException exception) {
            if (isNotFoundHttpStatus(exception.getStatusCode().value())) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.temporarilyUnavailable("抖音主数据源 HTTP 请求失败");
        } catch (WebClientRequestException exception) {
            return LiveSourceResult.temporarilyUnavailable("抖音主数据源网络请求失败");
        } catch (RuntimeException exception) {
            return LiveSourceResult.temporarilyUnavailable("抖音主数据源请求失败");
        }
    }

    /**
     * 按主播列表逐个查询状态。
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
                results.add(LiveSourceResult.unknown("抖音监控对象无效"));
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
     * 将数据源快照与已有主播资料合并，避免临时缺字段覆盖历史有效值。
     *
     * @param result 数据源结果
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
     * 请求直播页并读取 ttwid。
     *
     * @param roomId 网页房间标识
     * @return 页面响应摘要
     */
    private PageResponse requestRoomPage(String roomId) {
        return webClient.get()
                .uri(URI.create(buildRoomUrl(roomId)))
                .headers(headers -> {
                    headers.set(HttpHeaders.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
                    headers.set(HttpHeaders.REFERER, trimTrailingSlash(properties.getWebBaseUrl()) + "/");
                })
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new PageResponse(
                                response.statusCode().value(),
                                response.cookies().getFirst("ttwid"))))
                .block(timeout());
    }

    /**
     * 创建抖音主接口地址。
     *
     * @param roomId 网页房间标识
     * @return 主接口地址
     */
    private URI buildApiUri(String roomId) {
        return UriComponentsBuilder
                .fromUriString(buildUrl(properties.getApiBaseUrl(), API_PATH))
                .queryParam("aid", "6383")
                .queryParam("device_platform", "web")
                .queryParam("enter_from", "web_live")
                .queryParam("cookie_enabled", "true")
                .queryParam("browser_language", "zh-CN")
                .queryParam("browser_platform", "Win32")
                .queryParam("browser_name", "Chrome")
                .queryParam("browser_version", "109.0.0.0")
                .queryParam("web_rid", roomId)
                .build()
                .toUri();
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
     * 拼接根地址和路径。
     *
     * @param baseUrl 根地址
     * @param path 路径
     * @return 完整地址
     */
    private String buildUrl(String baseUrl, String path) {
        return trimTrailingSlash(baseUrl) + path;
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

    /**
     * 页面请求的最小响应信息。
     *
     * @param statusCode 页面 HTTP 状态码
     * @param ttwid 页面下发的 ttwid
     */
    private record PageResponse(int statusCode, ResponseCookie ttwid) {
    }
}

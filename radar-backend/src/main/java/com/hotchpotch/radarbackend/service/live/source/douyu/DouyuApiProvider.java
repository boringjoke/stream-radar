package com.hotchpotch.radarbackend.service.live.source.douyu;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.config.DouyuDataSourceProperties;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 斗鱼 betard 主数据源 Provider。
 */
@Component
public class DouyuApiProvider {

    /**
     * 斗鱼房间资料接口路径前缀。
     */
    private static final String BETARD_PATH = "/betard/";

    /**
     * 斗鱼数据源 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 斗鱼数据源配置。
     */
    private final DouyuDataSourceProperties properties;

    /**
     * 斗鱼主数据源解析器。
     */
    private final DouyuApiParser parser;

    /**
     * 创建斗鱼主数据源 Provider。
     *
     * @param webClient 斗鱼数据源 HTTP 客户端
     * @param properties 斗鱼数据源配置
     * @param parser 斗鱼主数据源解析器
     */
    public DouyuApiProvider(
            @Qualifier("douyuWebClient") WebClient webClient,
            DouyuDataSourceProperties properties,
            DouyuApiParser parser) {
        this.webClient = webClient;
        this.properties = properties;
        this.parser = parser;
    }

    /**
     * 查询单个斗鱼房间资料和状态。
     *
     * @param room URL 解析后的直播间身份
     * @return 主数据源结果
     */
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        if (room == null || isBlank(room.getRoomId())) {
            return LiveSourceResult.unknown("斗鱼房间标识为空");
        }

        String url = buildUrl(properties.getApiBaseUrl(), BETARD_PATH + room.getRoomId());
        try {
            String responseBody = webClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout());
            return parser.parse(responseBody, room.getRoomId());
        } catch (WebClientResponseException exception) {
            if (isNotFoundHttpStatus(exception.getStatusCode().value())) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.temporarilyUnavailable("斗鱼主数据源 HTTP 请求失败");
        } catch (WebClientRequestException exception) {
            return LiveSourceResult.temporarilyUnavailable("斗鱼主数据源网络请求失败");
        } catch (RuntimeException exception) {
            return LiveSourceResult.temporarilyUnavailable("斗鱼主数据源请求失败");
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
                results.add(LiveSourceResult.unknown("斗鱼监控对象无效"));
                continue;
            }
            ResolvedLiveRoom room = new ResolvedLiveRoom(
                    LivePlatform.DOUYU,
                    anchor.getRoomId(),
                    anchor.getRoomUrl());
            results.add(mergeWithExistingAnchor(resolve(room), anchor));
        }
        return results;
    }

    /**
     * 将数据源快照与已有资料合并，避免字段暂缺时覆盖最后一次有效资料。
     *
     * @param result 数据源结果
     * @param anchor 已保存主播
     * @return 合并后的结果
     */
    private LiveSourceResult mergeWithExistingAnchor(LiveSourceResult result, LiveAnchor anchor) {
        if (!result.isAvailable()) {
            return result;
        }
        LiveSnapshot snapshot = result.getSnapshot();
        LiveSnapshot merged = new LiveSnapshot(
                LivePlatform.DOUYU,
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
     * 创建请求超时时间。
     *
     * @return 请求超时时间
     */
    private Duration timeout() {
        return Duration.ofMillis(Math.max(1000, properties.getResponseTimeoutMs() + 1000L));
    }

    /**
     * 拼接数据源地址。
     *
     * @param baseUrl 根地址
     * @param path 路径
     * @return 完整地址
     */
    private String buildUrl(String baseUrl, String path) {
        return trimTrailingSlash(baseUrl) + path;
    }

    /**
     * 判断 HTTP 状态是否表示房间不存在。
     *
     * @param status HTTP 状态码
     * @return 是否为不存在状态
     */
    private boolean isNotFoundHttpStatus(int status) {
        return status == 404 || status == 410;
    }

    /**
     * 选择第一个非空文本。
     *
     * @param first 优先值
     * @param fallback 备用值
     * @return 选择结果
     */
    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }

    /**
     * 去除根地址末尾斜杠。
     *
     * @param value 根地址
     * @return 处理后的根地址
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

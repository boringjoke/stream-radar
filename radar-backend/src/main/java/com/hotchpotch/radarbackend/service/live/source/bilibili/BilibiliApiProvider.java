package com.hotchpotch.radarbackend.service.live.source.bilibili;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hotchpotch.radarbackend.config.BilibiliDataSourceProperties;
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
 * B站主数据源 Provider。
 */
@Component
public class BilibiliApiProvider {

    /**
     * 房间信息接口路径。
     */
    private static final String ROOM_INFO_PATH = "/room/v1/Room/get_info";

    /**
     * UID 批量状态接口路径。
     */
    private static final String STATUS_BATCH_PATH = "/room/v1/Room/get_status_info_by_uids";

    /**
     * B站数据源 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * B站数据源配置。
     */
    private final BilibiliDataSourceProperties properties;

    /**
     * B站 API 解析器。
     */
    private final BilibiliApiParser parser;

    /**
     * 创建B站 API Provider。
     *
     * @param webClient B站数据源 HTTP 客户端
     * @param properties B站数据源配置
     * @param parser B站 API 解析器
     */
    public BilibiliApiProvider(
            @Qualifier("bilibiliWebClient") WebClient webClient,
            BilibiliDataSourceProperties properties,
            BilibiliApiParser parser) {
        this.webClient = webClient;
        this.properties = properties;
        this.parser = parser;
    }

    /**
     * 查询单个房间信息，并尽量用 UID 批量接口补全主播资料。
     *
     * @param room URL 解析后的直播间身份
     * @return 主数据源结果
     */
    public LiveSourceResult resolve(ResolvedLiveRoom room) {
        LiveSourceResult roomInfoResult = requestRoomInfo(room.getRoomId());
        if (!roomInfoResult.isAvailable()) {
            return roomInfoResult;
        }

        LiveSnapshot roomSnapshot = roomInfoResult.getSnapshot();
        if (isBlank(roomSnapshot.getPlatformUid())) {
            return roomInfoResult;
        }

        Map<String, LiveSourceResult> statusResults = requestStatusBatch(
                List.of(roomSnapshot.getPlatformUid()));
        LiveSourceResult statusResult = statusResults.get(roomSnapshot.getPlatformUid());
        if (statusResult != null && statusResult.isAvailable()) {
            return LiveSourceResult.available(mergeSnapshots(roomSnapshot, statusResult.getSnapshot()));
        }

        // 房间信息接口已经确认房间存在，补全接口失败时保留可用的基础快照。
        return roomInfoResult;
    }

    /**
     * 批量查询已保存主播的状态。
     *
     * @param anchors 待查询主播列表
     * @return 与输入主播顺序对应的主数据源结果
     */
    public List<LiveSourceResult> queryStatus(List<LiveAnchor> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }

        Map<String, LiveSourceResult> statusResults = new LinkedHashMap<>();
        List<String> requestedUids = new ArrayList<>();
        Set<String> distinctUids = new LinkedHashSet<>();
        for (LiveAnchor anchor : anchors) {
            if (anchor != null && !isBlank(anchor.getPlatformUid())) {
                String uid = anchor.getPlatformUid().trim();
                if (isNumeric(uid)) {
                    distinctUids.add(uid);
                }
            }
        }
        requestedUids.addAll(distinctUids);
        if (!requestedUids.isEmpty()) {
            statusResults.putAll(requestStatusBatch(requestedUids));
        }

        List<LiveSourceResult> results = new ArrayList<>(anchors.size());
        for (LiveAnchor anchor : anchors) {
            if (anchor == null) {
                results.add(LiveSourceResult.unknown("B站监控对象为空"));
                continue;
            }
            String uid = anchor.getPlatformUid();
            if (!isBlank(uid) && isNumeric(uid)) {
                LiveSourceResult result = statusResults.get(uid.trim());
                results.add(result == null
                        ? LiveSourceResult.unknown("B站批量状态接口未返回该 UID")
                        : mergeWithExistingAnchor(result, anchor));
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
     * 请求B站房间信息接口。
     *
     * @param roomId 房间标识
     * @return 房间信息结果
     */
    private LiveSourceResult requestRoomInfo(String roomId) {
        if (isBlank(roomId)) {
            return LiveSourceResult.unknown("B站房间标识为空");
        }
        String url = buildUrl(ROOM_INFO_PATH) + "?room_id=" + roomId;
        try {
            String responseBody = webClient.get()
                    .uri(URI.create(url))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout());
            return parser.parseRoomInfo(responseBody, roomId);
        } catch (WebClientResponseException exception) {
            if (isNotFoundHttpStatus(exception.getStatusCode().value())) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.temporarilyUnavailable("B站房间信息接口 HTTP 请求失败");
        } catch (WebClientRequestException exception) {
            return LiveSourceResult.temporarilyUnavailable("B站房间信息接口网络请求失败");
        } catch (RuntimeException exception) {
            return LiveSourceResult.temporarilyUnavailable("B站房间信息接口请求失败");
        }
    }

    /**
     * 请求B站 UID 批量状态接口。
     *
     * @param uids 待查询 UID
     * @return 按 UID 对应的结果
     */
    private Map<String, LiveSourceResult> requestStatusBatch(List<String> uids) {
        if (uids == null || uids.isEmpty()) {
            return Map.of();
        }

        List<Long> numericUids = new ArrayList<>();
        for (String uid : uids) {
            try {
                numericUids.add(Long.valueOf(uid));
            } catch (NumberFormatException exception) {
                // 由调用方在结果阶段标记为 UNKNOWN，不将非法 UID 发给平台。
            }
        }
        if (numericUids.size() != uids.size()) {
            Map<String, LiveSourceResult> results = new LinkedHashMap<>();
            for (String uid : uids) {
                results.put(uid, LiveSourceResult.unknown("B站 UID 格式不合法"));
            }
            return results;
        }

        try {
            String responseBody = webClient.post()
                    .uri(URI.create(buildUrl(STATUS_BATCH_PATH)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("uids", numericUids))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(timeout());
            return parser.parseStatusBatch(responseBody, uids);
        } catch (WebClientResponseException exception) {
            LiveSourceResult result = LiveSourceResult.temporarilyUnavailable(
                    "B站批量状态接口 HTTP 请求失败");
            return fillResults(uids, result);
        } catch (WebClientRequestException exception) {
            LiveSourceResult result = LiveSourceResult.temporarilyUnavailable(
                    "B站批量状态接口网络请求失败");
            return fillResults(uids, result);
        } catch (RuntimeException exception) {
            LiveSourceResult result = LiveSourceResult.temporarilyUnavailable(
                    "B站批量状态接口请求失败");
            return fillResults(uids, result);
        }
    }

    /**
     * 将主数据源结果与已有实体资料合并，避免字段暂缺时覆盖最后一次有效资料。
     *
     * @param result 主数据源结果
     * @param anchor 已保存主播
     * @return 合并后的结果
     */
    private LiveSourceResult mergeWithExistingAnchor(LiveSourceResult result, LiveAnchor anchor) {
        if (!result.isAvailable()) {
            return result;
        }
        LiveSnapshot snapshot = result.getSnapshot();
        LiveSnapshot merged = new LiveSnapshot(
                LivePlatform.BILIBILI,
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
     * 合并房间信息和 UID 批量状态资料。
     *
     * @param roomSnapshot 房间信息快照
     * @param statusSnapshot 批量状态快照
     * @return 合并后的统一快照
     */
    private LiveSnapshot mergeSnapshots(LiveSnapshot roomSnapshot, LiveSnapshot statusSnapshot) {
        return new LiveSnapshot(
                LivePlatform.BILIBILI,
                firstNonBlank(statusSnapshot.getRoomId(), roomSnapshot.getRoomId()),
                firstNonBlank(statusSnapshot.getPlatformUid(), roomSnapshot.getPlatformUid()),
                firstNonBlank(statusSnapshot.getAnchorName(), roomSnapshot.getAnchorName()),
                firstNonBlank(statusSnapshot.getAvatarUrl(), roomSnapshot.getAvatarUrl()),
                firstNonBlank(statusSnapshot.getCoverUrl(), roomSnapshot.getCoverUrl()),
                firstNonBlank(statusSnapshot.getLiveTitle(), roomSnapshot.getLiveTitle()),
                statusSnapshot.getOnlineCount() == null
                        ? roomSnapshot.getOnlineCount()
                        : statusSnapshot.getOnlineCount(),
                statusSnapshot.getLiveStatus());
    }

    /**
     * 按 UID 创建相同的结果 Map。
     *
     * @param uids UID 列表
     * @param result 结果
     * @return 结果 Map
     */
    private Map<String, LiveSourceResult> fillResults(List<String> uids, LiveSourceResult result) {
        Map<String, LiveSourceResult> results = new LinkedHashMap<>();
        for (String uid : uids) {
            results.put(uid, result);
        }
        return results;
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
     * 拼接B站 API 地址。
     *
     * @param path API 路径
     * @return 完整地址
     */
    private String buildUrl(String path) {
        return trimTrailingSlash(properties.getApiBaseUrl()) + path;
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
     * 判断字符串是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 判断字符串是否为数字。
     *
     * @param value 待判断文本
     * @return 是否为数字
     */
    private boolean isNumeric(String value) {
        if (isBlank(value)) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 选择第一个非空文本。
     *
     * @param first 优先文本
     * @param fallback 备用文本
     * @return 选择结果
     */
    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
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

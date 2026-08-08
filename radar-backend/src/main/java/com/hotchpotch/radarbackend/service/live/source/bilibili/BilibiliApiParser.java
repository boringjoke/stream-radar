package com.hotchpotch.radarbackend.service.live.source.bilibili;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.springframework.stereotype.Component;

/**
 * B站 JSON 主数据源解析器。
 */
@Component
public class BilibiliApiParser {

    /**
     * 明确表示直播间不存在的B站业务码。
     */
    private static final List<Integer> NOT_FOUND_CODES = List.of(1, 60004);

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建B站 API 解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     */
    public BilibiliApiParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析房间信息接口响应。
     *
     * @param responseBody 接口响应正文
     * @param requestedRoomId 请求使用的房间标识，仅用于错误摘要
     * @return 统一数据源结果
     */
    public LiveSourceResult parseRoomInfo(String responseBody, String requestedRoomId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            return LiveSourceResult.unknown("B站房间信息响应不是有效 JSON");
        }
        if (root == null || !root.isObject()) {
            return LiveSourceResult.unknown("B站房间信息响应为空");
        }

        Integer code = integerValue(root.get("code"));
        if (code == null) {
            return LiveSourceResult.unknown("B站房间信息响应缺少业务码");
        }
        if (isNotFoundCode(code)) {
            return LiveSourceResult.notFound();
        }
        if (code != 0) {
            return LiveSourceResult.temporarilyUnavailable("B站房间信息接口返回业务异常");
        }

        JsonNode data = root.get("data");
        if (data == null || !data.isObject()) {
            return LiveSourceResult.unknown("B站房间信息响应缺少数据");
        }

        String roomId = textValue(data.get("room_id"));
        String platformUid = textValue(data.get("uid"));
        Integer liveStatus = integerValue(data.get("live_status"));
        if (isBlank(roomId) || isBlank(platformUid) || liveStatus == null) {
            return LiveSourceResult.unknown("B站房间信息响应缺少关键字段");
        }

        LiveStatus status = mapLiveStatus(liveStatus);
        if (status == null) {
            return LiveSourceResult.unknown("B站房间信息响应包含未知直播状态");
        }

        LiveSnapshot snapshot = new LiveSnapshot(
                LivePlatform.BILIBILI,
                roomId,
                platformUid,
                textValue(data.get("uname")),
                textValue(data.get("face")),
                firstTextValue(data, "cover_from_user", "user_cover", "cover"),
                textValue(data.get("title")),
                longValue(data.get("online")),
                status);
        return LiveSourceResult.available(snapshot);
    }

    /**
     * 解析B站 UID 批量状态接口响应。
     *
     * <p>响应中缺少某个请求 UID 时返回 UNKNOWN，不能将缺少记录解释为未开播。</p>
     *
     * @param responseBody 接口响应正文
     * @param requestedUids 请求的 UID 列表
     * @return 按 UID 对应的结果
     */
    public Map<String, LiveSourceResult> parseStatusBatch(
            String responseBody,
            List<String> requestedUids) {
        Map<String, LiveSourceResult> results = new LinkedHashMap<>();
        if (requestedUids == null || requestedUids.isEmpty()) {
            return results;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            fillResults(results, requestedUids,
                    LiveSourceResult.unknown("B站批量状态响应不是有效 JSON"));
            return results;
        }
        if (root == null || !root.isObject()) {
            fillResults(results, requestedUids,
                    LiveSourceResult.unknown("B站批量状态响应为空"));
            return results;
        }

        Integer code = integerValue(root.get("code"));
        if (code == null) {
            fillResults(results, requestedUids,
                    LiveSourceResult.unknown("B站批量状态响应缺少业务码"));
            return results;
        }
        if (code != 0) {
            LiveSourceResult result = isNotFoundCode(code)
                    ? LiveSourceResult.unknown("B站批量状态接口未返回有效数据")
                    : LiveSourceResult.temporarilyUnavailable("B站批量状态接口返回业务异常");
            fillResults(results, requestedUids, result);
            return results;
        }

        JsonNode data = root.get("data");
        for (String requestedUid : requestedUids) {
            JsonNode item = data != null && data.isObject() ? data.get(requestedUid) : null;
            if (item == null || !item.isObject()) {
                results.put(requestedUid,
                        LiveSourceResult.unknown("B站批量状态接口未返回该 UID"));
                continue;
            }
            results.put(requestedUid, parseStatusItem(item, requestedUid));
        }
        return results;
    }

    /**
     * 解析批量状态响应中的单个主播记录。
     *
     * @param item 单个主播 JSON 节点
     * @param requestedUid 请求的 UID
     * @return 单主播数据源结果
     */
    private LiveSourceResult parseStatusItem(JsonNode item, String requestedUid) {
        String roomId = textValue(item.get("room_id"));
        String platformUid = firstTextValue(item, "uid", null);
        if (isBlank(platformUid)) {
            platformUid = requestedUid;
        }
        Integer liveStatus = integerValue(item.get("live_status"));
        if (isBlank(roomId) || isBlank(platformUid) || liveStatus == null) {
            return LiveSourceResult.unknown("B站批量状态记录缺少关键字段");
        }

        LiveStatus status = mapLiveStatus(liveStatus);
        if (status == null) {
            return LiveSourceResult.unknown("B站批量状态记录包含未知直播状态");
        }

        LiveSnapshot snapshot = new LiveSnapshot(
                LivePlatform.BILIBILI,
                roomId,
                platformUid,
                textValue(item.get("uname")),
                textValue(item.get("face")),
                firstTextValue(item, "cover_from_user", "user_cover", "cover"),
                textValue(item.get("title")),
                longValue(item.get("online")),
                status);
        return LiveSourceResult.available(snapshot);
    }

    /**
     * 将B站直播状态转换为统一状态。
     *
     * @param liveStatus B站状态码
     * @return 统一状态，未知状态返回 null
     */
    private LiveStatus mapLiveStatus(Integer liveStatus) {
        return switch (liveStatus) {
            case 0, 2 -> LiveStatus.OFFLINE;
            case 1 -> LiveStatus.LIVE;
            default -> null;
        };
    }

    /**
     * 为每个请求 UID 填充相同的异常结果。
     *
     * @param results 结果 Map
     * @param requestedUids 请求 UID
     * @param result 异常结果
     */
    private void fillResults(
            Map<String, LiveSourceResult> results,
            List<String> requestedUids,
            LiveSourceResult result) {
        for (String requestedUid : requestedUids) {
            results.put(requestedUid, result);
        }
    }

    /**
     * 判断B站业务码是否明确表示不存在。
     *
     * @param code B站业务码
     * @return 是否为不存在业务码
     */
    private boolean isNotFoundCode(Integer code) {
        return NOT_FOUND_CODES.contains(code);
    }

    /**
     * 读取 JSON 文本字段。
     *
     * @param node JSON 节点
     * @return 非空文本，空值返回 null
     */
    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asString(null);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 按候选字段顺序读取第一个非空文本。
     *
     * @param parent JSON 父节点
     * @param fieldNames 候选字段名
     * @return 第一个非空字段值
     */
    private String firstTextValue(JsonNode parent, String... fieldNames) {
        if (parent == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (fieldName == null) {
                continue;
            }
            String value = textValue(parent.get(fieldName));
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 读取整数值。
     *
     * @param node JSON 节点
     * @return 整数值，无法读取时返回 null
     */
    private Integer integerValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Integer.valueOf(node.asString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 读取长整数值。
     *
     * @param node JSON 节点
     * @return 长整数值，无法读取时返回 null
     */
    private Long longValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return Long.valueOf(node.asString());
        } catch (NumberFormatException exception) {
            return null;
        }
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
}

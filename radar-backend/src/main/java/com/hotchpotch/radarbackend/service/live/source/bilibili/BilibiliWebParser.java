package com.hotchpotch.radarbackend.service.live.source.bilibili;

import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * B站直播间网页备用数据源解析器。
 */
@Component
public class BilibiliWebParser {

    /**
     * B站服务端注入直播间数据的全局变量名。
     */
    private static final String NEPTUNE_MARKER = "window.__NEPTUNE_IS_MY_WAIFU__";

    /**
     * 明确表示直播间不存在的页面业务码。
     */
    private static final List<Integer> NOT_FOUND_CODES = List.of(1, 60004);

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建B站网页解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     */
    public BilibiliWebParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析B站直播间 HTML。
     *
     * @param html 页面 HTML
     * @param requestedRoomId 请求使用的房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String html, String requestedRoomId) {
        String json = extractInjectedJson(html);
        if (json == null) {
            return LiveSourceResult.unknown("B站直播间页面缺少结构化直播数据");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception exception) {
            return LiveSourceResult.unknown("B站直播间页面内嵌数据不是有效 JSON");
        }
        if (root == null || !root.isObject()) {
            return LiveSourceResult.unknown("B站直播间页面内嵌数据为空");
        }

        JsonNode roomInitRes = root.get("roomInitRes");
        JsonNode roomInfoRes = root.get("roomInfoRes");
        Integer initCode = integerValue(roomInitRes == null ? null : roomInitRes.get("code"));
        Integer infoCode = integerValue(roomInfoRes == null ? null : roomInfoRes.get("code"));
        if (initCode == null || infoCode == null) {
            return LiveSourceResult.unknown("B站直播间页面缺少房间业务结果");
        }

        if (initCode != 0 || infoCode != 0) {
            if (isNotFoundCode(initCode) && isNotFoundCode(infoCode)) {
                return LiveSourceResult.notFound();
            }
            return LiveSourceResult.unknown("B站直播间页面返回异常业务结果");
        }

        JsonNode initData = roomInitRes.get("data");
        JsonNode infoData = roomInfoRes.get("data");
        JsonNode roomInfo = infoData == null ? null : infoData.get("room_info");
        JsonNode anchorBaseInfo = infoData == null
                ? null
                : infoData.path("anchor_info").path("base_info");
        if (initData == null || !initData.isObject()
                || roomInfo == null || !roomInfo.isObject()
                || anchorBaseInfo == null || !anchorBaseInfo.isObject()) {
            return LiveSourceResult.unknown("B站直播间页面缺少房间资料");
        }

        String initRoomId = textValue(initData.get("room_id"));
        String infoRoomId = textValue(roomInfo.get("room_id"));
        if (!isBlank(initRoomId) && !isBlank(infoRoomId) && !initRoomId.equals(infoRoomId)) {
            return LiveSourceResult.unknown("B站直播间页面房间标识不一致");
        }
        String roomId = isBlank(infoRoomId) ? initRoomId : infoRoomId;
        String platformUid = firstTextValue(roomInfo, "uid");
        if (isBlank(platformUid)) {
            platformUid = firstTextValue(initData, "uid");
        }
        Integer initStatus = integerValue(initData.get("live_status"));
        Integer infoStatus = integerValue(roomInfo.get("live_status"));
        if (initStatus != null && infoStatus != null && !initStatus.equals(infoStatus)) {
            return LiveSourceResult.unknown("B站直播间页面直播状态不一致");
        }
        Integer liveStatus = infoStatus == null ? initStatus : infoStatus;
        LiveStatus status = mapLiveStatus(liveStatus);
        if (isBlank(roomId) || isBlank(platformUid) || status == null) {
            return LiveSourceResult.unknown("B站直播间页面缺少关键字段");
        }

        LiveSnapshot snapshot = new LiveSnapshot(
                LivePlatform.BILIBILI,
                roomId,
                platformUid,
                textValue(anchorBaseInfo.get("uname")),
                textValue(anchorBaseInfo.get("face")),
                firstTextValue(roomInfo, "cover", "user_cover"),
                textValue(roomInfo.get("title")),
                longValue(roomInfo.get("online")),
                status);
        return LiveSourceResult.available(snapshot);
    }

    /**
     * 从 HTML 的 script 节点中提取 Neptune JSON。
     *
     * @param html 页面 HTML
     * @return JSON 文本，找不到时返回 null
     */
    private String extractInjectedJson(String html) {
        if (html == null || html.isBlank()) {
            return null;
        }
        Document document = Jsoup.parse(html);
        for (Element script : document.select("script")) {
            String scriptText = script.data();
            if (scriptText == null || scriptText.isBlank()) {
                scriptText = script.html();
            }
            String json = extractJsonAfterMarker(scriptText);
            if (json != null) {
                return json;
            }
        }
        return extractJsonAfterMarker(html);
    }

    /**
     * 从脚本文本中查找全局变量赋值后的 JSON 对象。
     *
     * @param source 脚本文本
     * @return JSON 对象文本
     */
    private String extractJsonAfterMarker(String source) {
        int searchFrom = 0;
        while (searchFrom < source.length()) {
            int markerIndex = source.indexOf(NEPTUNE_MARKER, searchFrom);
            if (markerIndex < 0) {
                return null;
            }
            int equalsIndex = source.indexOf('=', markerIndex + NEPTUNE_MARKER.length());
            if (equalsIndex < 0) {
                return null;
            }
            int objectStart = skipWhitespace(source, equalsIndex + 1);
            if (objectStart < source.length() && source.charAt(objectStart) == '{') {
                int objectEnd = findJsonObjectEnd(source, objectStart);
                if (objectEnd > objectStart) {
                    return source.substring(objectStart, objectEnd + 1);
                }
            }
            searchFrom = markerIndex + NEPTUNE_MARKER.length();
        }
        return null;
    }

    /**
     * 查找 JSON 对象的结束位置，忽略字符串内部的大括号。
     *
     * @param source 原始文本
     * @param objectStart JSON 对象起始位置
     * @return JSON 对象结束位置，无法匹配时返回 -1
     */
    private int findJsonObjectEnd(String source, int objectStart) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = objectStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }
            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    /**
     * 跳过 JSON 起始位置前的空白字符。
     *
     * @param source 原始文本
     * @param start 起始位置
     * @return 第一个非空白字符位置
     */
    private int skipWhitespace(String source, int start) {
        int index = start;
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * 将B站直播状态转换为统一状态。
     *
     * @param liveStatus B站状态码
     * @return 统一状态，未知状态返回 null
     */
    private LiveStatus mapLiveStatus(Integer liveStatus) {
        if (liveStatus == null) {
            return null;
        }
        return switch (liveStatus) {
            case 0, 2 -> LiveStatus.OFFLINE;
            case 1 -> LiveStatus.LIVE;
            default -> null;
        };
    }

    /**
     * 判断页面业务码是否明确表示不存在。
     *
     * @param code 页面业务码
     * @return 是否为不存在业务码
     */
    private boolean isNotFoundCode(Integer code) {
        return code != null && NOT_FOUND_CODES.contains(code);
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
        String value = node.asText(null);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 读取第一个非空文本字段。
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
            return Integer.valueOf(node.asText());
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
            return Long.valueOf(node.asText());
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

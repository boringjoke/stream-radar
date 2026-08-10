package com.hotchpotch.radarbackend.service.live.source.douyin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 抖音直播页面备用数据源解析器。
 *
 * <p>页面实际可能将状态对象放在转义后的脚本字符串中，且页面中可能同时存在空的
 * 初始 roomInfo 和后续填充完成的 roomInfo，因此解析时会扫描所有候选状态。</p>
 */
@Component
public class DouyinWebParser {

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建抖音页面解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     */
    public DouyinWebParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析抖音直播页面中的 roomStore.roomInfo。
     *
     * @param html 页面 HTML
     * @param requestedRoomId 请求使用的网页房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String html, String requestedRoomId) {
        if (isBlank(html)) {
            return LiveSourceResult.unknown("抖音直播页面响应为空");
        }
        if (isBlank(requestedRoomId)) {
            return LiveSourceResult.unknown("抖音页面房间标识为空");
        }

        String source = collectScriptSource(html);
        String normalizedSource = normalizeEscapedJson(source);
        LiveSourceResult candidateResult = null;
        int searchFrom = 0;
        while (searchFrom < normalizedSource.length()) {
            int roomStoreIndex = normalizedSource.indexOf("roomStore", searchFrom);
            if (roomStoreIndex < 0) {
                break;
            }
            int nextRoomStoreIndex = normalizedSource.indexOf("roomStore", roomStoreIndex + 1);
            int roomInfoIndex = normalizedSource.indexOf("\"roomInfo\"", roomStoreIndex);
            if (roomInfoIndex >= 0
                    && (nextRoomStoreIndex < 0 || roomInfoIndex < nextRoomStoreIndex)) {
                JsonNode roomInfo = parseRoomInfo(normalizedSource, roomInfoIndex);
                if (roomInfo != null) {
                    LiveSourceResult result = parseRoomInfo(roomInfo, requestedRoomId);
                    if (result != null && result.isAvailable()) {
                        return result;
                    }
                    if (result != null) {
                        candidateResult = result;
                    }
                }
            }
            searchFrom = roomStoreIndex + "roomStore".length();
        }

        return candidateResult == null
                ? LiveSourceResult.unknown("抖音页面未找到有效 roomStore.roomInfo")
                : candidateResult;
    }

    /**
     * 收集页面脚本内容，保留 HTML 作为没有 script 标签时的兼容输入。
     *
     * @param html 页面 HTML
     * @return 用于扫描的脚本文本
     */
    private String collectScriptSource(String html) {
        Document document = Jsoup.parse(html);
        StringBuilder scripts = new StringBuilder();
        for (Element script : document.select("script")) {
            String scriptText = script.data();
            if (isBlank(scriptText)) {
                scriptText = script.html();
            }
            if (!isBlank(scriptText)) {
                scripts.append(scriptText).append('\n');
            }
        }
        return scripts.length() == 0 ? html : scripts.toString();
    }

    /**
     * 将页面脚本中 JSON 字符串使用的转义引号还原为 JSON 引号。
     *
     * @param source 原始脚本文本
     * @return 可扫描 JSON 结构的文本
     */
    private String normalizeEscapedJson(String source) {
        StringBuilder normalized = new StringBuilder(source.length());
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '\\' && index + 1 < source.length()) {
                char next = source.charAt(index + 1);
                if (next == '"' || next == '\\') {
                    normalized.append(next);
                    index++;
                    continue;
                }
            }
            normalized.append(current);
        }
        return normalized.toString();
    }

    /**
     * 从 roomInfo 字段后提取 JSON 对象。
     *
     * @param source 已处理转义的脚本文本
     * @param roomInfoIndex roomInfo 字段位置
     * @return roomInfo JSON 节点
     */
    private JsonNode parseRoomInfo(String source, int roomInfoIndex) {
        int colonIndex = source.indexOf(':', roomInfoIndex + "\"roomInfo\"".length());
        if (colonIndex < 0) {
            return null;
        }
        int objectStart = skipWhitespace(source, colonIndex + 1);
        if (objectStart >= source.length() || source.charAt(objectStart) != '{') {
            return null;
        }
        int objectEnd = findJsonObjectEnd(source, objectStart);
        if (objectEnd <= objectStart) {
            return null;
        }
        try {
            return objectMapper.readTree(source.substring(objectStart, objectEnd + 1));
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 将 roomInfo 节点转换为统一快照。
     *
     * @param roomInfo roomInfo JSON 节点
     * @param requestedRoomId 请求使用的网页房间标识
     * @return 统一数据源结果
     */
    private LiveSourceResult parseRoomInfo(JsonNode roomInfo, String requestedRoomId) {
        JsonNode room = roomInfo.get("room");
        if (room == null || !room.isObject()) {
            return null;
        }

        String webRid = firstTextValue(roomInfo, "web_rid", "roomId");
        if (!isBlank(webRid) && !requestedRoomId.equals(webRid)) {
            return LiveSourceResult.unknown("抖音页面返回了不一致的房间标识");
        }

        LiveStatus status = mapLiveStatus(integerValue(room.get("status")));
        if (status == null) {
            return LiveSourceResult.unknown("抖音页面房间记录包含未知直播状态");
        }

        JsonNode anchor = roomInfo.get("anchor");
        if (anchor == null || !anchor.isObject()) {
            anchor = roomInfo.get("owner");
        }
        String platformUid = firstTextValue(anchor, "id_str", "uid", "user_id");
        String anchorName = firstTextValue(anchor, "nickname", "name");
        String avatarUrl = firstUrlValue(anchor == null ? null : anchor.get("avatar_thumb"));
        if (isBlank(avatarUrl)) {
            avatarUrl = firstTextValue(anchor, "avatar_url", "avatar");
        }

        LiveSnapshot snapshot = new LiveSnapshot(
                LivePlatform.DOUYIN,
                requestedRoomId,
                platformUid,
                anchorName,
                avatarUrl,
                firstUrlValue(room.get("cover")),
                firstTextValue(room, "title", "room_title"),
                parseOnlineCount(firstTextValue(room, "user_count_str", "user_count")),
                status);
        return LiveSourceResult.available(snapshot);
    }

    /**
     * 映射页面房间状态。
     *
     * @param status 抖音页面房间状态
     * @return 统一直播状态
     */
    private LiveStatus mapLiveStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 2 -> LiveStatus.LIVE;
            case 4 -> LiveStatus.OFFLINE;
            default -> null;
        };
    }

    /**
     * 读取对象中的第一个非空文本字段。
     *
     * @param parent JSON 父节点
     * @param fieldNames 候选字段名
     * @return 第一个非空字段值
     */
    private String firstTextValue(JsonNode parent, String... fieldNames) {
        if (parent == null || !parent.isObject() || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            if (isBlank(fieldName)) {
                continue;
            }
            JsonNode node = parent.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            String value = node.asString(null);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 读取对象中的第一张图片地址。
     *
     * @param imageNode 图片节点
     * @return 第一张图片地址
     */
    private String firstUrlValue(JsonNode imageNode) {
        if (imageNode == null || imageNode.isNull()) {
            return null;
        }
        if (imageNode.isArray()) {
            for (JsonNode item : imageNode) {
                String value = firstUrlValue(item);
                if (!isBlank(value)) {
                    return value;
                }
            }
            return null;
        }
        if (imageNode.isObject()) {
            return firstUrlValue(imageNode.get("url_list"));
        }
        String value = imageNode.asString(null);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 将抖音展示格式的观看人数转换为统一数值。
     *
     * @param value 抖音观看人数文本
     * @return 观看人数，无法转换时返回 null
     */
    private Long parseOnlineCount(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("+", "")
                .toLowerCase();
        try {
            if (normalized.endsWith("w") || normalized.endsWith("万")) {
                return multiplyCount(normalized.substring(0, normalized.length() - 1), 10000);
            }
            if (normalized.endsWith("k") || normalized.endsWith("千")) {
                return multiplyCount(normalized.substring(0, normalized.length() - 1), 1000);
            }
            if (normalized.endsWith("m")) {
                return multiplyCount(normalized.substring(0, normalized.length() - 1), 1000000);
            }
            if (normalized.endsWith("亿")) {
                return multiplyCount(normalized.substring(0, normalized.length() - 1), 100000000);
            }
            return Long.valueOf(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 计算带单位的观看人数。
     *
     * @param number 数字部分
     * @param multiplier 单位倍数
     * @return 转换后的数值
     */
    private Long multiplyCount(String number, int multiplier) {
        try {
            return Math.round(Double.parseDouble(number) * multiplier);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * 查找 JSON 对象结束位置，忽略字符串中的大括号。
     *
     * @param source JSON 文本
     * @param objectStart 对象开始位置
     * @return 对象结束位置，无法匹配时返回 -1
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
     * 跳过起始位置前的空白字符。
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
     * 读取整数节点。
     *
     * @param node JSON 节点
     * @return 整数值
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
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

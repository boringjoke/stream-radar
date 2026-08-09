package com.hotchpotch.radarbackend.service.live.source.huya;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 虎牙直播页面主数据源解析器。
 */
@Component
public class HuyaPageParser {

    /**
     * 虎牙页面内嵌直播数据的属性标记。
     */
    private static final Pattern STREAM_MARKER = Pattern.compile("\\bstream\\s*:");

    /**
     * JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建虎牙页面解析器。
     *
     * @param objectMapper JSON 解析器
     */
    public HuyaPageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析虎牙直播间页面中的内嵌 stream 数据。
     *
     * @param html 页面 HTML
     * @param requestedRoomId 请求房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String html, String requestedRoomId) {
        if (isBlank(html)) {
            return LiveSourceResult.unknown("虎牙直播间页面响应为空");
        }
        if (isBlank(requestedRoomId)) {
            return LiveSourceResult.unknown("虎牙房间标识为空");
        }

        Document document = Jsoup.parse(html);
        for (Element script : document.select("script")) {
            String scriptText = script.data();
            if (isBlank(scriptText)) {
                scriptText = script.html();
            }
            String streamJson = extractStreamJson(scriptText);
            if (streamJson == null) {
                continue;
            }
            try {
                JsonNode stream = objectMapper.readTree(streamJson.replace("undefined", "null"));
                LiveSourceResult result = parseStream(stream, requestedRoomId);
                if (result != null) {
                    return result;
                }
            } catch (Exception exception) {
                // 当前脚本片段无法解析时继续尝试其他脚本，最终仍按 UNKNOWN 处理。
            }
        }
        return LiveSourceResult.unknown("虎牙直播间页面缺少可解析的 stream 数据");
    }

    /**
     * 将 stream 对象转换为统一主播快照。
     *
     * @param stream stream JSON 对象
     * @param requestedRoomId 请求房间标识
     * @return 统一数据源结果；输入结构不符合约定时返回 null
     */
    private LiveSourceResult parseStream(JsonNode stream, String requestedRoomId) {
        if (stream == null || !stream.isObject()) {
            return null;
        }
        JsonNode data = stream.get("data");
        if (data == null || !data.isArray() || data.isEmpty()) {
            return null;
        }

        JsonNode liveData = null;
        for (JsonNode item : data) {
            if (item != null && item.isObject()
                    && item.get("gameStreamInfoList") != null
                    && item.get("gameLiveInfo") != null) {
                liveData = item;
                break;
            }
        }
        if (liveData == null) {
            return null;
        }

        JsonNode streamInfoList = liveData.get("gameStreamInfoList");
        JsonNode gameLiveInfo = liveData.get("gameLiveInfo");
        if (!streamInfoList.isArray() || !gameLiveInfo.isObject()) {
            return LiveSourceResult.unknown("虎牙页面缺少直播流或主播资料字段");
        }
        for (JsonNode streamInfo : streamInfoList) {
            if (streamInfo == null || !streamInfo.isObject()) {
                return LiveSourceResult.unknown("虎牙页面直播流字段格式不正确");
            }
        }

        String platformUid = normalizePlatformUid(
                firstTextValue(gameLiveInfo, "uid", "uid_str", "userId", "user_id"));
        String anchorName = firstTextValue(gameLiveInfo, "nick", "nickname", "anchorName");
        String avatarUrl = firstTextValue(gameLiveInfo, "avatar180", "avatar", "avatarUrl");
        String coverUrl = firstTextValue(
                gameLiveInfo, "screenshot", "roomCover", "room_pic", "cover", "cover_url");
        String liveTitle = firstTextValue(
                gameLiveInfo, "introduction", "roomName", "room_name", "title");
        if (isBlank(platformUid)
                && isBlank(anchorName)
                && isBlank(avatarUrl)
                && isBlank(coverUrl)
                && isBlank(liveTitle)) {
            return LiveSourceResult.unknown("虎牙页面主播资料字段均为空");
        }

        LiveStatus status = streamInfoList.isEmpty() ? LiveStatus.OFFLINE : LiveStatus.LIVE;
        Long onlineCount = firstLongValue(gameLiveInfo, "attendeeCount", "totalCount");
        return LiveSourceResult.available(new LiveSnapshot(
                LivePlatform.HUYA,
                requestedRoomId,
                platformUid,
                anchorName,
                avatarUrl,
                stripQuery(coverUrl),
                liveTitle,
                onlineCount,
                status));
    }

    /**
     * 提取 stream 属性值对应的 JSON 对象。
     *
     * @param source 脚本文本
     * @return JSON 对象文本
     */
    private String extractStreamJson(String source) {
        if (isBlank(source)) {
            return null;
        }
        Matcher matcher = STREAM_MARKER.matcher(source);
        while (matcher.find()) {
            int objectStart = skipWhitespace(source, matcher.end());
            if (objectStart >= source.length() || source.charAt(objectStart) != '{') {
                continue;
            }
            int objectEnd = findObjectEnd(source, objectStart);
            if (objectEnd > objectStart) {
                return source.substring(objectStart, objectEnd + 1);
            }
        }
        return null;
    }

    /**
     * 查找 JavaScript 对象结束位置，忽略字符串中的大括号。
     *
     * @param source 脚本文本
     * @param objectStart 对象起始位置
     * @return 对象结束位置，无法匹配时返回 -1
     */
    private int findObjectEnd(String source, int objectStart) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int index = objectStart; index < source.length(); index++) {
            char current = source.charAt(index);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '"' || current == '\'' || current == '`') {
                quote = current;
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
     * @param source 文本
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
     * 读取第一个非空文本字段。
     *
     * @param parent JSON 父节点
     * @param fieldNames 候选字段名
     * @return 第一个非空字段值
     */
    private String firstTextValue(JsonNode parent, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = parent.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            String value = node.asText(null);
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 过滤虎牙错误页返回的无效主播 UID 占位值。
     *
     * @param value 原始主播 UID
     * @return 有效主播 UID；空值或 0 占位值返回 null
     */
    private String normalizePlatformUid(String value) {
        return isBlank(value) || "0".equals(value) ? null : value;
    }

    /**
     * 读取第一个非空长整数值。
     *
     * @param parent JSON 父节点
     * @param fieldNames 候选字段名
     * @return 第一个可读取的数值
     */
    private Long firstLongValue(JsonNode parent, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode node = parent.get(fieldName);
            if (node == null || node.isNull()) {
                continue;
            }
            try {
                return Long.valueOf(node.asText());
            } catch (NumberFormatException exception) {
                // 当前字段不是数字时继续尝试下一个候选字段。
            }
        }
        return null;
    }

    /**
     * 移除封面地址后的查询参数。
     *
     * @param value 原始地址
     * @return 清理后的地址
     */
    private String stripQuery(String value) {
        if (isBlank(value)) {
            return value;
        }
        int queryIndex = value.indexOf('?');
        return queryIndex < 0 ? value : value.substring(0, queryIndex);
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

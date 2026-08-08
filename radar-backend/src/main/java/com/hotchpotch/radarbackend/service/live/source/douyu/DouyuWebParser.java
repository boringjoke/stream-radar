package com.hotchpotch.radarbackend.service.live.source.douyu;

import java.util.ArrayList;
import java.util.List;

import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 斗鱼直播间网页备用数据源解析器。
 *
 * <p>当前斗鱼网页使用 Next.js Flight 脚本，Parser 同时兼容已解码 JSON 和转义后的 Flight 片段。</p>
 */
@Component
public class DouyuWebParser {

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 斗鱼主数据源字段解析器，用于复用状态和资料映射规则。
     */
    private final DouyuApiParser apiParser;

    /**
     * 创建斗鱼网页解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     * @param apiParser 斗鱼主数据源字段解析器
     */
    public DouyuWebParser(ObjectMapper objectMapper, DouyuApiParser apiParser) {
        this.objectMapper = objectMapper;
        this.apiParser = apiParser;
    }

    /**
     * 解析斗鱼直播间网页。
     *
     * @param html 网页 HTML
     * @param requestedRoomId 请求房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String html, String requestedRoomId) {
        if (html == null || html.isBlank()) {
            return LiveSourceResult.unknown("斗鱼直播间网页响应为空");
        }

        Document document = Jsoup.parse(html);
        for (Element script : document.select("script")) {
            String scriptText = script.data();
            if (scriptText == null || scriptText.isBlank()) {
                scriptText = script.html();
            }
            for (String candidate : candidateTexts(scriptText)) {
                JsonNode room = extractRoomObject(candidate);
                if (room != null) {
                    return apiParser.parseRoom(room, requestedRoomId, "斗鱼网页备用数据源");
                }
            }
        }

        JsonNode room = extractRoomObject(html);
        if (room != null) {
            return apiParser.parseRoom(room, requestedRoomId, "斗鱼网页备用数据源");
        }
        return LiveSourceResult.unknown("斗鱼直播间网页缺少结构化房间数据");
    }

    /**
     * 枚举脚本原文和 Next.js Flight 字符串解码结果。
     *
     * @param source 脚本原文
     * @return 候选文本
     */
    private List<String> candidateTexts(String source) {
        List<String> candidates = new ArrayList<>();
        if (source == null || source.isBlank()) {
            return candidates;
        }
        candidates.add(source);

        int searchFrom = 0;
        while (searchFrom < source.length()) {
            int markerIndex = source.indexOf("self.__next_f.push", searchFrom);
            if (markerIndex < 0) {
                break;
            }
            int stringStart = source.indexOf('"', markerIndex);
            if (stringStart < 0) {
                break;
            }
            DecodedString decoded = readJavascriptString(source, stringStart);
            if (decoded == null) {
                break;
            }
            if (!decoded.value().isBlank()) {
                candidates.add(decoded.value());
            }
            searchFrom = decoded.endIndex() + 1;
        }

        // 兼容未被脚本字符串包装、但 JSON 双引号被反斜杠转义的页面片段。
        String unescaped = source.replace("\\\"", "\"");
        if (!unescaped.equals(source)) {
            candidates.add(unescaped);
        }
        return candidates;
    }

    /**
     * 从候选文本中提取 roomInfo.room JSON 对象。
     *
     * @param source 候选文本
     * @return 房间对象，找不到时返回 null
     */
    private JsonNode extractRoomObject(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        int searchFrom = 0;
        while (searchFrom < source.length()) {
            int roomInfoIndex = source.indexOf("\"roomInfo\"", searchFrom);
            if (roomInfoIndex < 0) {
                return null;
            }
            int roomKeyIndex = source.indexOf("\"room\"", roomInfoIndex + 10);
            if (roomKeyIndex < 0) {
                return null;
            }
            int colonIndex = source.indexOf(':', roomKeyIndex + 6);
            if (colonIndex < 0) {
                return null;
            }
            int objectStart = skipWhitespace(source, colonIndex + 1);
            if (objectStart < source.length() && source.charAt(objectStart) == '{') {
                int objectEnd = findJsonObjectEnd(source, objectStart);
                if (objectEnd > objectStart) {
                    try {
                        JsonNode room = objectMapper.readTree(source.substring(objectStart, objectEnd + 1));
                        if (room != null && room.isObject()) {
                            return room;
                        }
                    } catch (Exception exception) {
                        // 当前候选片段无法解析时继续尝试其他脚本片段。
                    }
                }
            }
            searchFrom = roomInfoIndex + 10;
        }
        return null;
    }

    /**
     * 读取一个 JavaScript 双引号字符串并解码常见转义。
     *
     * @param source 原始脚本文本
     * @param stringStart 起始双引号位置
     * @return 解码结果
     */
    private DecodedString readJavascriptString(String source, int stringStart) {
        StringBuilder value = new StringBuilder();
        for (int index = stringStart + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '"') {
                return new DecodedString(value.toString(), index);
            }
            if (current != '\\') {
                value.append(current);
                continue;
            }
            if (index + 1 >= source.length()) {
                return null;
            }
            char escaped = source.charAt(++index);
            switch (escaped) {
                case '"' -> value.append('"');
                case '\\' -> value.append('\\');
                case '/' -> value.append('/');
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (index + 4 >= source.length()) {
                        return null;
                    }
                    String hex = source.substring(index + 1, index + 5);
                    try {
                        value.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                    index += 4;
                }
                default -> value.append(escaped);
            }
        }
        return null;
    }

    /**
     * 查找 JSON 对象结束位置，忽略字符串中的大括号。
     *
     * @param source 原始文本
     * @param objectStart 对象起始位置
     * @return 对象结束位置
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
     * JavaScript 字符串解码结果。
     *
     * @param value 解码文本
     * @param endIndex 结束双引号位置
     */
    private record DecodedString(String value, int endIndex) {
    }
}

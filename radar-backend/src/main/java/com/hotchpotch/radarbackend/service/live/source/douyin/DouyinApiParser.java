package com.hotchpotch.radarbackend.service.live.source.douyin;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 抖音直播 JSON 主数据源解析器。
 */
@Component
public class DouyinApiParser {

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建抖音主数据源解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     */
    public DouyinApiParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析抖音直播接口响应。
     *
     * @param responseBody 接口响应正文
     * @param requestedRoomId 请求使用的网页房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String responseBody, String requestedRoomId) {
        if (isBlank(responseBody)) {
            return LiveSourceResult.unknown("抖音主数据源响应为空");
        }
        if (isBlank(requestedRoomId)) {
            return LiveSourceResult.unknown("抖音房间标识为空");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            return LiveSourceResult.unknown("抖音主数据源响应不是有效 JSON");
        }
        if (root == null || !root.isObject()) {
            return LiveSourceResult.unknown("抖音主数据源响应为空对象");
        }

        Integer statusCode = integerValue(root.get("status_code"));
        if (statusCode == null) {
            return LiveSourceResult.unknown("抖音主数据源响应缺少业务状态码");
        }
        if (statusCode != 0) {
            return LiveSourceResult.unknown("抖音主数据源返回业务异常");
        }

        JsonNode data = root.get("data");
        if (data == null || !data.isObject()) {
            return LiveSourceResult.unknown("抖音主数据源响应缺少数据对象");
        }

        JsonNode rooms = data.get("data");
        if (rooms == null || !rooms.isArray() || rooms.isEmpty()) {
            // 当前接口可能返回 status_code=0 但没有房间记录，不能据此判定未开播。
            return LiveSourceResult.unknown("抖音主数据源未返回房间记录");
        }

        JsonNode room = rooms.get(0);
        if (room == null || !room.isObject()) {
            return LiveSourceResult.unknown("抖音主数据源房间记录格式无效");
        }

        LiveStatus status = resolveLiveStatus(data, room);
        if (status == null) {
            return LiveSourceResult.unknown("抖音主数据源包含未知直播状态");
        }

        JsonNode user = data.get("user");
        String platformUid = firstTextValue(user, "id_str", "open_id_str", "sec_uid");
        String anchorName = firstTextValue(user, "nickname", "name");
        String avatarUrl = firstUrlValue(user == null ? null : user.get("avatar_thumb"));
        if (isBlank(avatarUrl)) {
            avatarUrl = firstTextValue(user, "avatar_url", "avatar");
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
     * 结合接口外层状态和房间记录状态判断统一直播状态。
     *
     * @param data 接口 data 对象
     * @param room 房间记录
     * @return 统一直播状态，无法确认时返回 null
     */
    private LiveStatus resolveLiveStatus(JsonNode data, JsonNode room) {
        LiveStatus outerStatus = mapOuterRoomStatus(integerValue(data.get("room_status")));
        LiveStatus recordStatus = mapRoomRecordStatus(integerValue(room.get("status")));
        if (outerStatus != null && recordStatus != null && outerStatus != recordStatus) {
            return null;
        }
        return outerStatus == null ? recordStatus : outerStatus;
    }

    /**
     * 映射接口 data.room_status。
     *
     * <p>当前已验证样本中 0 表示直播中，2 表示未开播。</p>
     *
     * @param roomStatus 抖音接口外层状态
     * @return 统一直播状态
     */
    private LiveStatus mapOuterRoomStatus(Integer roomStatus) {
        if (roomStatus == null) {
            return null;
        }
        return switch (roomStatus) {
            case 0 -> LiveStatus.LIVE;
            case 2 -> LiveStatus.OFFLINE;
            default -> null;
        };
    }

    /**
     * 映射接口 data.data[0].status。
     *
     * <p>当前已验证样本中 2 表示直播中，4 表示未开播。</p>
     *
     * @param roomStatus 房间记录状态
     * @return 统一直播状态
     */
    private LiveStatus mapRoomRecordStatus(Integer roomStatus) {
        if (roomStatus == null) {
            return null;
        }
        return switch (roomStatus) {
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
        return new BigDecimal(number)
                .multiply(BigDecimal.valueOf(multiplier))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
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

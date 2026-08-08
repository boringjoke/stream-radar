package com.hotchpotch.radarbackend.service.live.source.douyu;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 斗鱼 betard JSON 主数据源解析器。
 */
@Component
public class DouyuApiParser {

    /**
     * Jackson JSON 解析器。
     */
    private final ObjectMapper objectMapper;

    /**
     * 创建斗鱼主数据源解析器。
     *
     * @param objectMapper Jackson JSON 解析器
     */
    public DouyuApiParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 解析斗鱼 betard 响应。
     *
     * @param responseBody 接口响应正文
     * @param requestedRoomId 请求使用的房间标识
     * @return 统一数据源结果
     */
    public LiveSourceResult parse(String responseBody, String requestedRoomId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            return LiveSourceResult.unknown("斗鱼主数据源响应不是有效 JSON");
        }
        if (root == null || !root.isObject()) {
            return LiveSourceResult.unknown("斗鱼主数据源响应为空");
        }

        JsonNode room = root.get("room");
        if (room == null || !room.isObject()) {
            // betard 对无效房间可能返回 HTML 错误页或缺少 room 的响应，不能直接当作不存在。
            return LiveSourceResult.unknown("斗鱼主数据源缺少房间对象");
        }
        return parseRoom(room, requestedRoomId, "斗鱼主数据源");
    }

    /**
     * 将房间对象转换为统一快照。
     *
     * @param room 斗鱼房间对象
     * @param requestedRoomId 请求房间标识
     * @param sourceName 数据源名称
     * @return 统一数据源结果
     */
    public LiveSourceResult parseRoom(JsonNode room, String requestedRoomId, String sourceName) {
        String responseRoomId = firstTextValue(room, "room_id", "roomId");
        String roomId = isBlank(responseRoomId) ? requestedRoomId : responseRoomId;
        if (isBlank(roomId)) {
            return LiveSourceResult.unknown(sourceName + "缺少房间标识");
        }
        if (!isBlank(requestedRoomId) && !requestedRoomId.equals(roomId)) {
            return LiveSourceResult.unknown(sourceName + "返回了不一致的房间标识");
        }

        Integer showStatus = integerValue(room.get("show_status"));
        LiveStatus liveStatus = mapLiveStatus(showStatus);
        if (liveStatus == null) {
            return LiveSourceResult.unknown(sourceName + "缺少或包含未知直播状态");
        }

        JsonNode avatarNode = room.get("avatar");
        String avatarUrl = firstTextValue(room, "avatar_small", "avatar_mid", "avatar_url");
        if (isBlank(avatarUrl) && avatarNode != null && avatarNode.isObject()) {
            avatarUrl = firstTextValue(avatarNode, "small", "middle", "big");
        } else if (isBlank(avatarUrl) && avatarNode != null) {
            avatarUrl = textValue(avatarNode);
        }

        Long onlineCount = firstLongValue(room, "online", "online_count", "online_num", "hn");
        if (onlineCount == null) {
            // 斗鱼页面展示的是平台热度，当前 betard 响应位于 room_biz_all.hot，不能当作真实同时在线人数解读。
            onlineCount = firstLongValue(room.get("room_biz_all"), "hot");
        }

        LiveSnapshot snapshot = new LiveSnapshot(
                LivePlatform.DOUYU,
                roomId,
                firstTextValue(room, "owner_uid", "uid", "owner_id"),
                firstTextValue(room, "nickname", "owner_name", "anchor_name"),
                avatarUrl,
                firstTextValue(room, "room_pic", "room_src", "cover", "cover_url", "screenshot"),
                firstTextValue(room, "room_name", "title", "roomName"),
                onlineCount,
                liveStatus);
        return LiveSourceResult.available(snapshot);
    }

    /**
     * 将斗鱼状态转换为统一状态。
     *
     * <p>在线探测样本显示 1 为直播中、2 为未直播；0 作为旧响应兼容值按未直播处理，其他值保持未知。</p>
     *
     * @param showStatus 斗鱼状态值
     * @return 统一直播状态
     */
    private LiveStatus mapLiveStatus(Integer showStatus) {
        if (showStatus == null) {
            return null;
        }
        return switch (showStatus) {
            case 1 -> LiveStatus.LIVE;
            case 0, 2 -> LiveStatus.OFFLINE;
            default -> null;
        };
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
     * 读取第一个非空长整数值。
     *
     * @param parent JSON 父节点
     * @param fieldNames 候选字段名
     * @return 第一个可读取的数值
     */
    private Long firstLongValue(JsonNode parent, String... fieldNames) {
        if (parent == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            Long value = longValue(parent.get(fieldName));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * 读取 JSON 文本字段。
     *
     * @param node JSON 节点
     * @return 非空文本
     */
    private String textValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return isBlank(value) ? null : value.trim();
    }

    /**
     * 读取 JSON 整数值。
     *
     * @param node JSON 节点
     * @return 整数值
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
     * 读取 JSON 长整数值。
     *
     * @param node JSON 节点
     * @return 长整数值
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
     * 判断文本是否为空。
     *
     * @param value 待判断文本
     * @return 是否为空
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

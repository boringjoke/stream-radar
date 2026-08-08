package com.hotchpotch.radarbackend.service.live.source.douyu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 斗鱼主备数据源解析器定向验证。
 */
class DouyuParserTest {

    private final DouyuApiParser apiParser = new DouyuApiParser(new ObjectMapper());
    private final DouyuWebParser webParser = new DouyuWebParser(new ObjectMapper(), apiParser);

    @Test
    void shouldParseApiLiveAndOfflineStatus() {
        LiveSourceResult live = apiParser.parse(
                "{\"room\":{\"room_id\":9999,\"owner_uid\":204389,"
                        + "\"nickname\":\"主播\",\"avatar\":{\"small\":\"avatar\"},"
                        + "\"room_name\":\"标题\",\"room_pic\":\"cover\","
                        + "\"room_biz_all\":{\"hot\":\"3620629\"},"
                        + "\"show_status\":1}}",
                "9999");
        LiveSourceResult offline = apiParser.parse(
                "{\"room\":{\"room_id\":9998,\"owner_uid\":267512,"
                        + "\"nickname\":\"未直播主播\",\"show_status\":2}}",
                "9998");

        assertEquals(LiveSourceStatus.AVAILABLE, live.getStatus());
        assertNotNull(live.getSnapshot());
        assertEquals(LiveStatus.LIVE, live.getSnapshot().getLiveStatus());
        assertEquals("204389", live.getSnapshot().getPlatformUid());
        assertEquals("avatar", live.getSnapshot().getAvatarUrl());
        assertEquals(3620629L, live.getSnapshot().getOnlineCount());
        assertEquals(LiveStatus.OFFLINE, offline.getSnapshot().getLiveStatus());
    }

    @Test
    void shouldParseNextFlightRoomInfo() {
        String payload = "{\"roomInfo\":{\"room\":{\"room_id\":9999,"
                + "\"owner_uid\":204389,\"nickname\":\"主播\","
                + "\"avatar_mid\":\"avatar\",\"room_name\":\"标题\","
                + "\"room_pic\":\"cover\",\"room_biz_all\":{\"hot\":\"3620629\"},"
                + "\"show_status\":1}}}";
        String escapedPayload = payload.replace("\\", "\\\\").replace("\"", "\\\"");
        String html = "<html><body><script>self.__next_f.push([1,\""
                + escapedPayload
                + "\"])</script></body></html>";

        LiveSourceResult result = webParser.parse(html, "9999");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
        assertEquals("主播", result.getSnapshot().getAnchorName());
        assertEquals("cover", result.getSnapshot().getCoverUrl());
        assertEquals(3620629L, result.getSnapshot().getOnlineCount());
    }

    @Test
    void shouldKeepUnstructuredResponsesUnknown() {
        LiveSourceResult apiResult = apiParser.parse("<html><div class=error></div></html>", "999999999");
        LiveSourceResult webResult = webParser.parse(
                "<html><body><div class=error>暂时无法加载</div></body></html>",
                "999999999");

        assertEquals(LiveSourceStatus.UNKNOWN, apiResult.getStatus());
        assertEquals(LiveSourceStatus.UNKNOWN, webResult.getStatus());
    }
}

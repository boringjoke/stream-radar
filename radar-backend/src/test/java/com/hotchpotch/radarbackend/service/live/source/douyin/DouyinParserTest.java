package com.hotchpotch.radarbackend.service.live.source.douyin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 抖音主备解析器定向验证。
 */
class DouyinParserTest {

    private final DouyinApiParser apiParser = new DouyinApiParser(new ObjectMapper());
    private final DouyinWebParser webParser = new DouyinWebParser(new ObjectMapper());

    @Test
    void shouldParseApiLiveRoomAndMapDisplayedCount() {
        LiveSourceResult result = apiParser.parse(
                "{\"status_code\":0,\"data\":{"
                        + "\"room_status\":0,"
                        + "\"user\":{\"id_str\":\"1001\",\"nickname\":\"主播\","
                        + "\"avatar_thumb\":{\"url_list\":[\"avatar?token=1\"]}},"
                        + "\"data\":[{\"id_str\":\"room-internal-id\",\"status\":2,"
                        + "\"status_str\":\"2\",\"title\":\"直播标题\","
                        + "\"cover\":{\"url_list\":[\"cover?token=1\"]},"
                        + "\"user_count_str\":\"1w+\"}]}}",
                "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertNotNull(result.getSnapshot());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
        assertEquals("998", result.getSnapshot().getRoomId());
        assertEquals("1001", result.getSnapshot().getPlatformUid());
        assertEquals("主播", result.getSnapshot().getAnchorName());
        assertEquals("avatar?token=1", result.getSnapshot().getAvatarUrl());
        assertEquals("cover?token=1", result.getSnapshot().getCoverUrl());
        assertEquals("直播标题", result.getSnapshot().getLiveTitle());
        assertEquals(10000L, result.getSnapshot().getOnlineCount());
    }

    @Test
    void shouldParseApiOfflineRoom() {
        LiveSourceResult result = apiParser.parse(
                "{\"status_code\":0,\"data\":{"
                        + "\"room_status\":2,"
                        + "\"user\":{\"id_str\":\"1002\",\"nickname\":\"离线主播\"},"
                        + "\"data\":[{\"id_str\":\"room-internal-id\",\"status\":4,"
                        + "\"title\":\"上次直播\",\"user_count_str\":\"0\"}]}}",
                "999");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.OFFLINE, result.getSnapshot().getLiveStatus());
        assertEquals("离线主播", result.getSnapshot().getAnchorName());
        assertEquals(0L, result.getSnapshot().getOnlineCount());
    }

    @Test
    void shouldKeepEmptyRoomListUnknownInsteadOfOffline() {
        LiveSourceResult result = apiParser.parse(
                "{\"status_code\":0,\"data\":{\"room_status\":2,"
                        + "\"user\":{\"nickname\":\"待确认主播\"},\"data\":[]}}",
                "2025");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepBusinessErrorUnknownInsteadOfNotFound() {
        LiveSourceResult result = apiParser.parse(
                "{\"status_code\":4001038,\"data\":{\"prompts\":\"error\"}}",
                "999999999999999999");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldScanPastEmptyRoomStoreCandidateInWebPage() {
        String emptyState = "{\"roomStore\":{\"roomInfo\":{"
                + "\"web_rid\":\"998\",\"web_stream_url\":null}}}";
        String validState = "{\"roomStore\":{\"roomInfo\":{\"room\":{"
                + "\"id_str\":\"room-internal-id\",\"status\":2,"
                + "\"title\":\"页面直播标题\",\"user_count_str\":\"1234\","
                + "\"cover\":{\"url_list\":[\"cover?token=1\"]}},"
                + "\"web_rid\":\"998\",\"anchor\":{\"id_str\":\"1001\","
                + "\"nickname\":\"页面主播\",\"avatar_thumb\":{"
                + "\"url_list\":[\"avatar?token=1\"]}}}}}";
        String html = "<html><script>var first=\""
                + escapeJsonString(emptyState)
                + "\";</script><script>var second=\""
                + escapeJsonString(validState)
                + "\";</script></html>";

        LiveSourceResult result = webParser.parse(html, "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
        assertEquals("页面主播", result.getSnapshot().getAnchorName());
        assertEquals("页面直播标题", result.getSnapshot().getLiveTitle());
        assertEquals("cover?token=1", result.getSnapshot().getCoverUrl());
        assertEquals(1234L, result.getSnapshot().getOnlineCount());
    }

    @Test
    void shouldParseWebOfflineRoom() {
        String state = "{\"roomStore\":{\"roomInfo\":{\"room\":{"
                + "\"id_str\":\"room-internal-id\",\"status\":4,"
                + "\"title\":\"页面历史标题\",\"user_count_str\":\"0\"},"
                + "\"web_rid\":\"999\",\"anchor\":{\"nickname\":\"离线页面主播\"}}}}";
        String html = "<html><script>window.__STATE__=\""
                + escapeJsonString(state)
                + "\";</script></html>";

        LiveSourceResult result = webParser.parse(html, "999");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.OFFLINE, result.getSnapshot().getLiveStatus());
        assertEquals("离线页面主播", result.getSnapshot().getAnchorName());
    }

    @Test
    void shouldKeepWebPageWithoutRoomObjectUnknown() {
        String state = "{\"roomStore\":{\"roomInfo\":{"
                + "\"web_rid\":\"999999999999999999\",\"web_stream_url\":null}}}";
        String html = "<html><script>var state=\""
                + escapeJsonString(state)
                + "\";</script></html>";

        LiveSourceResult result = webParser.parse(html, "999999999999999999");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepMalformedWebPageUnknown() {
        LiveSourceResult result = webParser.parse(
                "<html><script>var state=\\\"{\\\"roomStore\\\":{</script></html>",
                "998");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    private String escapeJsonString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

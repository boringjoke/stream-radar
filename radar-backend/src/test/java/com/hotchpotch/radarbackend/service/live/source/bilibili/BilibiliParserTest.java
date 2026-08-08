package com.hotchpotch.radarbackend.service.live.source.bilibili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;

import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * B站主备数据源解析器定向验证。
 */
class BilibiliParserTest {

    private final BilibiliApiParser apiParser = new BilibiliApiParser(new ObjectMapper());
    private final BilibiliWebParser webParser = new BilibiliWebParser(new ObjectMapper());

    @Test
    void shouldParseApiRoomInfoAndNotFoundCode() {
        LiveSourceResult available = apiParser.parseRoomInfo(
                "{\"code\":0,\"data\":{\"uid\":672328094,\"room_id\":22637261,"
                        + "\"live_status\":1,\"uname\":\"主播\",\"face\":\"avatar\","
                        + "\"user_cover\":\"cover\",\"title\":\"标题\",\"online\":12}}",
                "22637261");
        LiveSourceResult notFound = apiParser.parseRoomInfo(
                "{\"code\":1,\"message\":\"room not found\",\"data\":null}",
                "999999999");

        assertEquals(LiveSourceStatus.AVAILABLE, available.getStatus());
        assertNotNull(available.getSnapshot());
        assertEquals(LiveStatus.LIVE, available.getSnapshot().getLiveStatus());
        assertEquals("672328094", available.getSnapshot().getPlatformUid());
        assertEquals(LiveSourceStatus.NOT_FOUND, notFound.getStatus());
    }

    @Test
    void shouldTreatMissingUidFromBatchResponseAsUnknown() {
        Map<String, LiveSourceResult> results = apiParser.parseStatusBatch(
                "{\"code\":0,\"data\":{\"672328094\":{\"uid\":672328094,"
                        + "\"room_id\":22637261,\"live_status\":0,\"title\":\"标题\"}}}",
                List.of("672328094", "474595627"));

        assertEquals(LiveSourceStatus.AVAILABLE, results.get("672328094").getStatus());
        assertEquals(LiveStatus.OFFLINE, results.get("672328094").getSnapshot().getLiveStatus());
        assertEquals(LiveSourceStatus.UNKNOWN, results.get("474595627").getStatus());
    }

    @Test
    void shouldParseNeptuneHtmlAndRejectUnstructuredPageAsUnknown() {
        String html = "<html><body><script>window.__NEPTUNE_IS_MY_WAIFU__="
                + "{\"roomInitRes\":{\"code\":0,\"data\":{\"room_id\":22637261,"
                + "\"live_status\":1}},\"roomInfoRes\":{\"code\":0,\"data\":{"
                + "\"room_info\":{\"room_id\":22637261,\"uid\":672328094,"
                + "\"live_status\":1,\"title\":\"标题\",\"cover\":\"cover\",\"online\":88},"
                + "\"anchor_info\":{\"base_info\":{\"uname\":\"主播\",\"face\":\"avatar\"}}}}};"
                + "</script></body></html>";

        LiveSourceResult available = webParser.parse(html, "22637261");
        LiveSourceResult unknown = webParser.parse("<html><title>直播间</title></html>", "13");

        assertEquals(LiveSourceStatus.AVAILABLE, available.getStatus());
        assertEquals(LiveStatus.LIVE, available.getSnapshot().getLiveStatus());
        assertEquals(LiveSourceStatus.UNKNOWN, unknown.getStatus());
    }
}

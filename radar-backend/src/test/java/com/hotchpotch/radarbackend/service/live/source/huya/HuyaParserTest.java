package com.hotchpotch.radarbackend.service.live.source.huya;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hotchpotch.radarbackend.domain.enums.LiveStatus;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceResult;
import com.hotchpotch.radarbackend.service.live.source.LiveSourceStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * 虎牙主备数据源解析器定向验证。
 */
class HuyaParserTest {

    private final HuyaPageParser pageParser = new HuyaPageParser(new ObjectMapper());
    private final HuyaDomParser domParser = new HuyaDomParser();

    @Test
    void shouldParseLiveStreamPage() {
        LiveSourceResult result = pageParser.parse(
                "<html><script>var page={stream: {\"data\":[{"
                        + "\"gameStreamInfoList\":[{\"sStreamName\":\"stream\"}],"
                        + "\"gameLiveInfo\":{\"uid\":\"1001\",\"nick\":\"主播\","
                        + "\"avatar180\":\"avatar\",\"roomName\":\"标题\","
                        + "\"introduction\":\"直播标题\",\"attendeeCount\":\"123456\","
                        + "\"totalCount\":\"654321\","
                        + "\"screenshot\":\"cover?token=1\"}}]}};</script></html>",
                "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertNotNull(result.getSnapshot());
        assertEquals(LiveStatus.LIVE, result.getSnapshot().getLiveStatus());
        assertEquals("1001", result.getSnapshot().getPlatformUid());
        assertEquals("主播", result.getSnapshot().getAnchorName());
        assertEquals("直播标题", result.getSnapshot().getLiveTitle());
        assertEquals(123456L, result.getSnapshot().getOnlineCount());
        assertEquals("cover", result.getSnapshot().getCoverUrl());
    }

    @Test
    void shouldFallbackToTotalCountWhenAttendeeCountIsMissing() {
        LiveSourceResult result = pageParser.parse(
                "<html><script>var page={stream: {\"data\":[{"
                        + "\"gameStreamInfoList\":[{\"sStreamName\":\"stream\"}],"
                        + "\"gameLiveInfo\":{\"introduction\":\"直播标题\","
                        + "\"totalCount\":98765}}]}};</script></html>",
                "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(98765L, result.getSnapshot().getOnlineCount());
    }

    @Test
    void shouldMapStructuredEmptyStreamListToOffline() {
        LiveSourceResult result = pageParser.parse(
                "<html><script>var page={stream: {\"data\":[{"
                        + "\"gameStreamInfoList\":[],"
                        + "\"gameLiveInfo\":{\"nick\":\"未直播主播\","
                        + "\"roomName\":\"上次直播标题\"}}]}};</script></html>",
                "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.OFFLINE, result.getSnapshot().getLiveStatus());
    }

    @Test
    void shouldKeepEmptyOrPlaceholderAnchorInfoUnknownInsteadOfOffline() {
        LiveSourceResult result = pageParser.parse(
                "<html><script>var page={stream: {\"data\":[{"
                        + "\"gameStreamInfoList\":[],"
                        + "\"gameLiveInfo\":{\"uid\":\"0\",\"nick\":\"\","
                        + "\"avatar180\":\"\",\"roomName\":\"\","
                        + "\"introduction\":\"\",\"attendeeCount\":\"\","
                        + "\"totalCount\":\"\"}}]}};</script></html>",
                "9989999999");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepMissingOrMalformedPageUnknown() {
        LiveSourceResult missing = pageParser.parse(
                "<html><title>虎牙直播</title></html>", "999999999");
        LiveSourceResult malformed = pageParser.parse(
                "<html><script>var page={stream: {data:[};</script></html>",
                "999999999");

        assertEquals(LiveSourceStatus.UNKNOWN, missing.getStatus());
        assertEquals(LiveSourceStatus.UNKNOWN, malformed.getStatus());
    }

    @Test
    void shouldKeepMissingAnchorDomPageUnknown() {
        LiveSourceResult result = domParser.parse(
                "<html><head><title>虎牙直播-年轻人喜爱的弹幕式互动直播平台</title></head>"
                        + "<body><div>哎呀，虎牙君找不到这个主播，要不搜索看看？</div>"
                        + "<meta property=\"og:image\""
                        + " content=\"generic-cover\"></body></html>",
                "99999999");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldKeepGenericDomErrorPageUnknownWithoutAnchorIdentity() {
        LiveSourceResult result = domParser.parse(
                "<html><head><title>官方直播间--虎牙直播</title>"
                        + "<meta name=\"description\" content=\"通用错误页描述\">"
                        + "<meta property=\"og:image\" content=\"generic-cover\"></head>"
                        + "<body></body></html>",
                "9989999999");

        assertEquals(LiveSourceStatus.UNKNOWN, result.getStatus());
    }

    @Test
    void shouldParseDomFallbackWithoutTurningUnknownStatusIntoOffline() {
        LiveSourceResult result = domParser.parse(
                "<html><head><meta property=\"og:title\" content=\"DOM标题\">"
                        + "<meta property=\"og:image\" content=\"cover?x=1\"></head>"
                        + "<body><div data-anchor-name=\"DOM主播\"></div>"
                        + "<div data-live-status=\"unknown\"></div></body></html>",
                "998");

        assertEquals(LiveSourceStatus.AVAILABLE, result.getStatus());
        assertEquals(LiveStatus.UNKNOWN, result.getSnapshot().getLiveStatus());
        assertEquals("DOM主播", result.getSnapshot().getAnchorName());
        assertEquals("cover", result.getSnapshot().getCoverUrl());
    }
}

package com.hotchpotch.radarbackend.service.live.source.huya;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 虎牙房间不存在结构化信号定向验证。
 */
class HuyaRoomNotFoundSignalTest {

    @Test
    void shouldRecognizeRoomNotFoundRedirect() {
        assertTrue(HuyaRoomNotFoundSignal.isRoomNotFoundRedirect(
                302,
                "https://www.huya.com/error?errorType=ROOM_NOT_FOUND"));
        assertTrue(HuyaRoomNotFoundSignal.isRoomNotFoundRedirect(
                301,
                "/error?foo=1&errorType=ROOM_NOT_FOUND"));
    }

    @Test
    void shouldIgnoreNonRoomNotFoundResponses() {
        assertFalse(HuyaRoomNotFoundSignal.isRoomNotFoundRedirect(
                200,
                "https://www.huya.com/error?errorType=ROOM_NOT_FOUND"));
        assertFalse(HuyaRoomNotFoundSignal.isRoomNotFoundRedirect(
                302,
                "https://www.huya.com/error?errorType=TEMPORARILY_UNAVAILABLE"));
        assertFalse(HuyaRoomNotFoundSignal.isRoomNotFoundRedirect(
                302,
                "https://www.huya.com/error"));
    }
}

package com.hotchpotch.radarbackend.service.live.url;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;

/**
 * URL 解析后的直播间身份。
 *
 * <p>该对象只表达 URL 中可以确认的身份，不代表平台已经确认房间真实存在。</p>
 */
public final class ResolvedLiveRoom {

    /**
     * 直播平台。
     */
    private final LivePlatform platform;

    /**
     * 平台直播间标识。
     */
    private final String roomId;

    /**
     * 规范化后的直播间地址。
     */
    private final String roomUrl;

    /**
     * 创建 URL 解析结果。
     *
     * @param platform 直播平台
     * @param roomId 平台直播间标识
     * @param roomUrl 规范化后的直播间地址
     */
    public ResolvedLiveRoom(LivePlatform platform, String roomId, String roomUrl) {
        this.platform = platform;
        this.roomId = roomId;
        this.roomUrl = roomUrl;
    }

    public LivePlatform getPlatform() {
        return platform;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getRoomUrl() {
        return roomUrl;
    }
}

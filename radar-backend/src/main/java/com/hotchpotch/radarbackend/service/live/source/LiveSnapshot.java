package com.hotchpotch.radarbackend.service.live.source;

import java.util.Objects;

import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.enums.LiveStatus;

/**
 * 平台数据源转换后的统一主播快照。
 */
public final class LiveSnapshot {

    /**
     * 直播平台。
     */
    private final LivePlatform platform;

    /**
     * 平台规范房间标识。
     */
    private final String roomId;

    /**
     * 平台主播用户标识。
     */
    private final String platformUid;

    /**
     * 主播名称。
     */
    private final String anchorName;

    /**
     * 主播头像地址。
     */
    private final String avatarUrl;

    /**
     * 直播封面地址。
     */
    private final String coverUrl;

    /**
     * 当前或最后一次有效直播标题。
     */
    private final String liveTitle;

    /**
     * 当前观看人数或平台提供的人气值。
     */
    private final Long onlineCount;

    /**
     * 统一直播状态。
     */
    private final LiveStatus liveStatus;

    /**
     * 创建统一直播快照。
     *
     * @param platform 直播平台
     * @param roomId 规范房间标识
     * @param platformUid 平台主播用户标识
     * @param anchorName 主播名称
     * @param avatarUrl 主播头像地址
     * @param coverUrl 直播封面地址
     * @param liveTitle 直播标题
     * @param onlineCount 观看人数
     * @param liveStatus 统一直播状态
     */
    public LiveSnapshot(
            LivePlatform platform,
            String roomId,
            String platformUid,
            String anchorName,
            String avatarUrl,
            String coverUrl,
            String liveTitle,
            Long onlineCount,
            LiveStatus liveStatus) {
        this.platform = Objects.requireNonNull(platform, "platform must not be null");
        this.roomId = roomId;
        this.platformUid = platformUid;
        this.anchorName = anchorName;
        this.avatarUrl = avatarUrl;
        this.coverUrl = coverUrl;
        this.liveTitle = liveTitle;
        this.onlineCount = onlineCount;
        this.liveStatus = Objects.requireNonNull(liveStatus, "liveStatus must not be null");
    }

    public LivePlatform getPlatform() {
        return platform;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getPlatformUid() {
        return platformUid;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getLiveTitle() {
        return liveTitle;
    }

    public Long getOnlineCount() {
        return onlineCount;
    }

    public LiveStatus getLiveStatus() {
        return liveStatus;
    }
}

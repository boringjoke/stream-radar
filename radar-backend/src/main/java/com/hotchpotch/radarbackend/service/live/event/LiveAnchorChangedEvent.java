package com.hotchpotch.radarbackend.service.live.event;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;

/**
 * 主播资料或直播状态发生语义变化后的内部事件。
 */
public final class LiveAnchorChangedEvent {

    /**
     * 主播主键。
     */
    private final Long anchorId;

    /**
     * 已落库的统一主播快照。
     */
    private final LiveSnapshot snapshot;

    /**
     * 已落库的规范直播间地址。
     */
    private final String roomUrl;

    /**
     * 已落库的最后检测时间。
     */
    private final LocalDateTime lastCheckTime;

    /**
     * 创建主播变化事件。
     *
     * @param anchorId 主播主键
     * @param snapshot 已落库的统一快照
     * @param roomUrl 已落库的规范直播间地址
     * @param lastCheckTime 已落库的最后检测时间
     */
    public LiveAnchorChangedEvent(
            Long anchorId,
            LiveSnapshot snapshot,
            String roomUrl,
            LocalDateTime lastCheckTime) {
        this.anchorId = Objects.requireNonNull(anchorId, "anchorId must not be null");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.roomUrl = roomUrl;
        this.lastCheckTime = lastCheckTime;
    }

    public Long getAnchorId() {
        return anchorId;
    }

    public LiveSnapshot getSnapshot() {
        return snapshot;
    }

    public String getRoomUrl() {
        return roomUrl;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }
}

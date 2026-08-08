package com.hotchpotch.radarbackend.service.live.validation;

import java.util.Objects;

import com.hotchpotch.radarbackend.service.live.source.LiveSnapshot;

/**
 * 平台数据源对直播间存在性的校验结果。
 */
public final class LiveRoomCheckResult {

    /**
     * 房间存在性校验状态。
     */
    private final LiveRoomCheckStatus status;

    /**
     * 数据源确认房间存在时返回的主播快照。
     */
    private final LiveSnapshot snapshot;

    /**
     * 无法确认时的错误摘要。
     */
    private final String message;

    private LiveRoomCheckResult(
            LiveRoomCheckStatus status,
            LiveSnapshot snapshot,
            String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.snapshot = snapshot;
        this.message = message;
    }

    /**
     * 创建指定状态的校验结果。
     *
     * @param status 校验状态
     * @return 校验结果
     */
    public static LiveRoomCheckResult of(LiveRoomCheckStatus status) {
        return new LiveRoomCheckResult(status, null, null);
    }

    /**
     * 创建带快照的校验结果。
     *
     * @param status 校验状态
     * @param snapshot 统一主播快照
     * @return 校验结果
     */
    public static LiveRoomCheckResult of(LiveRoomCheckStatus status, LiveSnapshot snapshot) {
        return new LiveRoomCheckResult(status, snapshot, null);
    }

    /**
     * 创建带错误摘要的校验结果。
     *
     * @param status 校验状态
     * @param message 错误摘要
     * @return 校验结果
     */
    public static LiveRoomCheckResult of(LiveRoomCheckStatus status, String message) {
        return new LiveRoomCheckResult(status, null, message);
    }

    /**
     * 创建“房间存在”的校验结果。
     *
     * @return 校验结果
     */
    public static LiveRoomCheckResult available() {
        return of(LiveRoomCheckStatus.AVAILABLE);
    }

    /**
     * 创建带主播快照的“房间存在”结果。
     *
     * @param snapshot 统一主播快照
     * @return 校验结果
     */
    public static LiveRoomCheckResult available(LiveSnapshot snapshot) {
        return of(LiveRoomCheckStatus.AVAILABLE,
                Objects.requireNonNull(snapshot, "snapshot must not be null"));
    }

    /**
     * 创建“房间不存在”的校验结果。
     *
     * @return 校验结果
     */
    public static LiveRoomCheckResult notFound() {
        return of(LiveRoomCheckStatus.NOT_FOUND);
    }

    /**
     * 获取校验状态。
     *
     * @return 校验状态
     */
    public LiveRoomCheckStatus getStatus() {
        return status;
    }

    public LiveSnapshot getSnapshot() {
        return snapshot;
    }

    public String getMessage() {
        return message;
    }
}

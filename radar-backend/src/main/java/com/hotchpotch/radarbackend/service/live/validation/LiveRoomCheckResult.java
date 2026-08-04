package com.hotchpotch.radarbackend.service.live.validation;

import java.util.Objects;

/**
 * 平台数据源对直播间存在性的校验结果。
 */
public final class LiveRoomCheckResult {

    /**
     * 房间存在性校验状态。
     */
    private final LiveRoomCheckStatus status;

    private LiveRoomCheckResult(LiveRoomCheckStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * 创建指定状态的校验结果。
     *
     * @param status 校验状态
     * @return 校验结果
     */
    public static LiveRoomCheckResult of(LiveRoomCheckStatus status) {
        return new LiveRoomCheckResult(status);
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
}

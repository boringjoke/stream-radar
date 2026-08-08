package com.hotchpotch.radarbackend.service.live.source;

import java.util.Objects;

/**
 * 单次数据源查询结果。
 */
public final class LiveSourceResult {

    /**
     * 数据源结果状态。
     */
    private final LiveSourceStatus status;

    /**
     * 数据源成功时的统一快照。
     */
    private final LiveSnapshot snapshot;

    /**
     * 面向日志和路由判断的错误摘要，不包含响应正文或 Cookie。
     */
    private final String message;

    private LiveSourceResult(LiveSourceStatus status, LiveSnapshot snapshot, String message) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.snapshot = snapshot;
        this.message = message;
    }

    /**
     * 创建成功结果。
     *
     * @param snapshot 统一直播快照
     * @return 成功结果
     */
    public static LiveSourceResult available(LiveSnapshot snapshot) {
        return new LiveSourceResult(LiveSourceStatus.AVAILABLE,
                Objects.requireNonNull(snapshot, "snapshot must not be null"), null);
    }

    /**
     * 创建明确不存在结果。
     *
     * @return 房间不存在结果
     */
    public static LiveSourceResult notFound() {
        return new LiveSourceResult(LiveSourceStatus.NOT_FOUND, null, null);
    }

    /**
     * 创建暂时不可用结果。
     *
     * @param message 错误摘要
     * @return 暂时不可用结果
     */
    public static LiveSourceResult temporarilyUnavailable(String message) {
        return new LiveSourceResult(LiveSourceStatus.TEMPORARILY_UNAVAILABLE, null, message);
    }

    /**
     * 创建无法确认结果。
     *
     * @param message 无法确认原因摘要
     * @return 无法确认结果
     */
    public static LiveSourceResult unknown(String message) {
        return new LiveSourceResult(LiveSourceStatus.UNKNOWN, null, message);
    }

    public LiveSourceStatus getStatus() {
        return status;
    }

    public LiveSnapshot getSnapshot() {
        return snapshot;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAvailable() {
        return status == LiveSourceStatus.AVAILABLE && snapshot != null;
    }
}

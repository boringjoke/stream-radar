package com.hotchpotch.radarbackend.service.live.source;

/**
 * 单次直播数据源查询结果状态。
 */
public enum LiveSourceStatus {

    /**
     * 已获取到可以使用的直播快照。
     */
    AVAILABLE,

    /**
     * 数据源明确确认房间不存在。
     */
    NOT_FOUND,

    /**
     * 数据源暂时不可用，例如超时、限流或服务端错误。
     */
    TEMPORARILY_UNAVAILABLE,

    /**
     * 响应存在但无法可靠判断房间或直播状态。
     */
    UNKNOWN
}

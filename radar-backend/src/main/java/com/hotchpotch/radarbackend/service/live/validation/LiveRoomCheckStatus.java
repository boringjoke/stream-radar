package com.hotchpotch.radarbackend.service.live.validation;

/**
 * 平台数据源对直播间存在性的校验结果。
 */
public enum LiveRoomCheckStatus {

    /**
     * 已确认直播间存在。
     */
    AVAILABLE,

    /**
     * 已确认直播间不存在。
     */
    NOT_FOUND,

    /**
     * 平台暂时不可用，无法完成校验。
     */
    TEMPORARILY_UNAVAILABLE,

    /**
     * 数据源暂时无法可靠判断。
     */
    UNKNOWN
}

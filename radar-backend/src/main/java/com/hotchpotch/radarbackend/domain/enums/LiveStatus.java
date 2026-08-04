package com.hotchpotch.radarbackend.domain.enums;

/**
 * 统一直播状态。
 */
public enum LiveStatus {

    /**
     * 已确认直播中。
     */
    LIVE("LIVE"),

    /**
     * 已确认未直播。
     */
    OFFLINE("OFFLINE"),

    /**
     * 当前无法可靠判断状态。
     */
    UNKNOWN("UNKNOWN"),

    /**
     * 数据源请求或解析失败。
     */
    ERROR("ERROR");

    /**
     * 数据库存储的状态标识。
     */
    private final String code;

    LiveStatus(String code) {
        this.code = code;
    }

    /**
     * 获取数据库状态标识。
     *
     * @return 状态标识
     */
    public String getCode() {
        return code;
    }
}

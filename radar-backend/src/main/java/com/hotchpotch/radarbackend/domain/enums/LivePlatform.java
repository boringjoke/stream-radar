package com.hotchpotch.radarbackend.domain.enums;

import java.util.Arrays;
import java.util.Optional;

/**
 * StreamRadar 支持的直播平台。
 */
public enum LivePlatform {

    /**
     * 哔哩哔哩直播。
     */
    BILIBILI("BILIBILI", "https://live.bilibili.com"),

    /**
     * 斗鱼直播。
     */
    DOUYU("DOUYU", "https://www.douyu.com"),

    /**
     * 虎牙直播。
     */
    HUYA("HUYA", "https://www.huya.com"),

    /**
     * 抖音直播。
     */
    DOUYIN("DOUYIN", "https://live.douyin.com");

    /**
     * 数据库存储的平台标识。
     */
    private final String code;

    /**
     * 平台规范化直播间地址前缀。
     */
    private final String canonicalUrlPrefix;

    LivePlatform(String code, String canonicalUrlPrefix) {
        this.code = code;
        this.canonicalUrlPrefix = canonicalUrlPrefix;
    }

    /**
     * 获取数据库平台标识。
     *
     * @return 平台标识
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取规范化直播间地址前缀。
     *
     * @return 规范化地址前缀
     */
    public String getCanonicalUrlPrefix() {
        return canonicalUrlPrefix;
    }

    /**
     * 根据数据库平台标识查询枚举。
     *
     * @param code 平台标识
     * @return 对应平台
     */
    public static Optional<LivePlatform> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(platform -> platform.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}

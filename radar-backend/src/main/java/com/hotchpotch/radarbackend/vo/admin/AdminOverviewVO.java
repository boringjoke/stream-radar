package com.hotchpotch.radarbackend.vo.admin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理中心全平台统计响应。
 */
public class AdminOverviewVO {

    /**
     * 启用普通用户数量。
     */
    private final long userCount;

    /**
     * 纳入统计范围的主播数量。
     */
    private final long anchorCount;

    /**
     * 被启用普通用户关注的去重主播数量。
     */
    private final long followedAnchorCount;

    /**
     * 四个平台分别对应的主播数量。
     */
    private final Map<String, Long> platformAnchorCounts;

    /**
     * 创建管理中心统计响应。
     *
     * @param userCount 启用普通用户数量
     * @param anchorCount 纳入统计范围的主播数量
     * @param followedAnchorCount 被启用普通用户关注的主播数量
     * @param platformAnchorCounts 平台主播数量
     */
    public AdminOverviewVO(
            long userCount,
            long anchorCount,
            long followedAnchorCount,
            Map<String, Long> platformAnchorCounts) {
        this.userCount = userCount;
        this.anchorCount = anchorCount;
        this.followedAnchorCount = followedAnchorCount;
        this.platformAnchorCounts = platformAnchorCounts == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(platformAnchorCounts));
    }

    public long getUserCount() {
        return userCount;
    }

    public long getAnchorCount() {
        return anchorCount;
    }

    public long getFollowedAnchorCount() {
        return followedAnchorCount;
    }

    public Map<String, Long> getPlatformAnchorCounts() {
        return platformAnchorCounts;
    }
}

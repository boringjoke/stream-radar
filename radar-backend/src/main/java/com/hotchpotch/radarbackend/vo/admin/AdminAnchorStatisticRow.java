package com.hotchpotch.radarbackend.vo.admin;

/**
 * 管理中心主播统计中间记录。
 */
public class AdminAnchorStatisticRow {

    /**
     * 平台标识。
     */
    private String platform;

    /**
     * 平台直播间标识。
     */
    private String roomId;

    /**
     * 启用普通用户的去重关注人数。
     */
    private Long followerCount;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }
}

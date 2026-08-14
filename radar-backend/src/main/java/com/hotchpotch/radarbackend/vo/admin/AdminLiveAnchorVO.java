package com.hotchpotch.radarbackend.vo.admin;

/**
 * 管理中心主播分页记录。
 */
public class AdminLiveAnchorVO {

    /**
     * 主播主键。
     */
    private Long anchorId;

    /**
     * 平台标识：BILIBILI、DOUYU、HUYA、DOUYIN。
     */
    private String platform;

    /**
     * 平台直播间标识。
     */
    private String roomId;

    /**
     * 规范化直播间地址。
     */
    private String roomUrl;

    /**
     * 主播名称。
     */
    private String anchorName;

    /**
     * 主播头像地址。
     */
    private String avatarUrl;

    /**
     * 当前主播的去重关注用户数。
     */
    private Long followerCount;

    public Long getAnchorId() {
        return anchorId;
    }

    public void setAnchorId(Long anchorId) {
        this.anchorId = anchorId;
    }

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

    public String getRoomUrl() {
        return roomUrl;
    }

    public void setRoomUrl(String roomUrl) {
        this.roomUrl = roomUrl;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }
}

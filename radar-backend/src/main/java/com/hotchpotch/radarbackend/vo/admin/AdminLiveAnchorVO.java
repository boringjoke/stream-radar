package com.hotchpotch.radarbackend.vo.admin;

import java.time.LocalDateTime;

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
     * 平台主播用户标识。
     */
    private String platformUid;

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
     * 直播封面地址。
     */
    private String coverUrl;

    /**
     * 当前或最后一次有效直播标题。
     */
    private String liveTitle;

    /**
     * 当前观看人数或平台人气值。
     */
    private Long onlineCount;

    /**
     * 直播状态：LIVE、OFFLINE、UNKNOWN、ERROR。
     */
    private String liveStatus;

    /**
     * 最后检测时间。
     */
    private LocalDateTime lastCheckTime;

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

    public String getPlatformUid() {
        return platformUid;
    }

    public void setPlatformUid(String platformUid) {
        this.platformUid = platformUid;
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getLiveTitle() {
        return liveTitle;
    }

    public void setLiveTitle(String liveTitle) {
        this.liveTitle = liveTitle;
    }

    public Long getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(Long onlineCount) {
        this.onlineCount = onlineCount;
    }

    public String getLiveStatus() {
        return liveStatus;
    }

    public void setLiveStatus(String liveStatus) {
        this.liveStatus = liveStatus;
    }

    public LocalDateTime getLastCheckTime() {
        return lastCheckTime;
    }

    public void setLastCheckTime(LocalDateTime lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }

    public Long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Long followerCount) {
        this.followerCount = followerCount;
    }
}

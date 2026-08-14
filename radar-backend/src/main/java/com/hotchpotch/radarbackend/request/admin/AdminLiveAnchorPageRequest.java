package com.hotchpotch.radarbackend.request.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 管理员主播分页查询请求。
 */
public class AdminLiveAnchorPageRequest {

    /**
     * 当前页码，从 1 开始。
     */
    @Min(value = 1, message = "页码必须大于 0")
    private Integer pageNum = 1;

    /**
     * 每页记录数，取值范围为 1～100。
     */
    @Min(value = 1, message = "每页记录数必须大于 0")
    @Max(value = 100, message = "每页记录数不能超过 100")
    private Integer pageSize = 20;

    /**
     * 平台精确筛选条件。
     */
    private String platform;

    /**
     * 主播名称模糊筛选条件。
     */
    private String anchorName;

    /**
     * 房间号精确筛选条件。
     */
    private String roomId;

    /**
     * 关注人数最小值，包含边界。
     */
    @Min(value = 0, message = "关注人数最小值不能小于 0")
    private Long minFollowerCount;

    /**
     * 关注人数最大值，包含边界。
     */
    @Min(value = 0, message = "关注人数最大值不能小于 0")
    private Long maxFollowerCount;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAnchorName() {
        return anchorName;
    }

    public void setAnchorName(String anchorName) {
        this.anchorName = anchorName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Long getMinFollowerCount() {
        return minFollowerCount;
    }

    public void setMinFollowerCount(Long minFollowerCount) {
        this.minFollowerCount = minFollowerCount;
    }

    public Long getMaxFollowerCount() {
        return maxFollowerCount;
    }

    public void setMaxFollowerCount(Long maxFollowerCount) {
        this.maxFollowerCount = maxFollowerCount;
    }
}

package com.hotchpotch.radarbackend.request.live;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 取消单个关注请求。
 */
public class LiveUnfollowRequest {

    /**
     * 关注关系主键，由后端关注列表返回。
     */
    @NotNull(message = "关注关系标识不能为空")
    @Positive(message = "关注关系标识必须大于 0")
    private Long followId;

    public Long getFollowId() {
        return followId;
    }

    public void setFollowId(Long followId) {
        this.followId = followId;
    }
}

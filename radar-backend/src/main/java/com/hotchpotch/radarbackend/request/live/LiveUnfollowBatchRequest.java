package com.hotchpotch.radarbackend.request.live;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 批量取消关注请求。
 */
public class LiveUnfollowBatchRequest {

    /**
     * 待取消的关注关系主键列表，全部由后端关注列表返回。
     */
    @NotEmpty(message = "至少选择一位主播")
    @Size(max = 100, message = "单次最多取消 100 位主播")
    @Valid
    private List<@NotNull(message = "关注关系标识不能为空") @Positive(message = "关注关系标识必须大于 0") Long> followIds;

    public List<Long> getFollowIds() {
        return followIds;
    }

    public void setFollowIds(List<Long> followIds) {
        this.followIds = followIds;
    }
}

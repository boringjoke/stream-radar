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
}

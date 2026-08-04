package com.hotchpotch.radarbackend.vo.common;

import java.util.List;

/**
 * 通用分页响应对象。
 *
 * @param <T> 分页记录类型
 */
public class PageVO<T> {

    /**
     * 当前页码，从 1 开始。
     */
    private final Integer pageNum;

    /**
     * 每页记录数。
     */
    private final Integer pageSize;

    /**
     * 符合查询条件的总记录数。
     */
    private final Long total;

    /**
     * 当前页记录列表。
     */
    private final List<T> records;

    /**
     * 创建分页响应对象。
     *
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     * @param total 符合查询条件的总记录数
     * @param records 当前页记录列表
     */
    public PageVO(Integer pageNum, Integer pageSize, Long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records == null ? List.of() : records;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public List<T> getRecords() {
        return records;
    }
}

package com.hotchpotch.radarbackend.service.admin;

import java.util.List;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.request.admin.AdminLiveAnchorPageRequest;
import com.hotchpotch.radarbackend.vo.admin.AdminLiveAnchorVO;
import com.hotchpotch.radarbackend.vo.common.PageVO;
import org.springframework.stereotype.Service;

/**
 * 管理中心主播业务服务。
 */
@Service
public class AdminLiveAnchorService {

    /**
     * 默认页码。
     */
    private static final int DEFAULT_PAGE_NUM = 1;

    /**
     * 默认每页记录数。
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页记录数。
     */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * 直播主播数据访问仓库。
     */
    private final LiveAnchorRepository liveAnchorRepository;

    /**
     * 创建管理中心主播业务服务。
     *
     * @param liveAnchorRepository 直播主播数据访问仓库
     */
    public AdminLiveAnchorService(LiveAnchorRepository liveAnchorRepository) {
        this.liveAnchorRepository = liveAnchorRepository;
    }

    /**
     * 分页查询当前仍被用户关注的主播及关注人数。
     *
     * @param request 分页查询请求
     * @return 管理中心主播分页结果
     */
    public PageVO<AdminLiveAnchorVO> page(AdminLiveAnchorPageRequest request) {
        int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        long total = liveAnchorRepository.countMonitoredLiveAnchors();
        long offset = calculateOffset(pageNum, pageSize);
        List<AdminLiveAnchorVO> records = offset >= total
                ? List.of()
                : liveAnchorRepository.findMonitoredLiveAnchorPage(offset, pageSize);
        return new PageVO<>(pageNum, pageSize, total, records);
    }

    /**
     * 规范化页码并校验页码范围。
     *
     * @param pageNum 原始页码
     * @return 可用于查询的页码
     */
    private int normalizePageNum(Integer pageNum) {
        int normalizedPageNum = pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
        if (normalizedPageNum < 1) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "页码必须大于 0");
        }
        return normalizedPageNum;
    }

    /**
     * 规范化每页记录数并校验上限。
     *
     * @param pageSize 原始每页记录数
     * @return 可用于查询的每页记录数
     */
    private int normalizePageSize(Integer pageSize) {
        int normalizedPageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (normalizedPageSize < 1 || normalizedPageSize > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "每页记录数必须在 1～100 之间");
        }
        return normalizedPageSize;
    }

    /**
     * 计算数据库分页偏移量。
     *
     * @param pageNum 当前页码
     * @param pageSize 每页记录数
     * @return 数据库查询偏移量
     */
    private long calculateOffset(int pageNum, int pageSize) {
        return (long) (pageNum - 1) * pageSize;
    }
}

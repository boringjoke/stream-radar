package com.hotchpotch.radarbackend.service.admin;

import java.util.List;
import java.util.Locale;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
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
     * 分页查询全部主播及当前启用普通用户的关注人数。
     *
     * @param request 分页查询请求
     * @return 管理中心主播分页结果
     */
    public PageVO<AdminLiveAnchorVO> page(AdminLiveAnchorPageRequest request) {
        int pageNum = normalizePageNum(request == null ? null : request.getPageNum());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        String platform = normalizePlatform(request == null ? null : request.getPlatform());
        String anchorName = normalizeLikeKeyword(request == null ? null : request.getAnchorName());
        String roomId = normalizeExactKeyword(request == null ? null : request.getRoomId());
        Long minFollowerCount = request == null ? null : request.getMinFollowerCount();
        Long maxFollowerCount = request == null ? null : request.getMaxFollowerCount();
        validateFollowerRange(minFollowerCount, maxFollowerCount);

        long total = liveAnchorRepository.countAdminLiveAnchors(
                platform,
                anchorName,
                roomId,
                minFollowerCount,
                maxFollowerCount);
        long offset = calculateOffset(pageNum, pageSize);
        List<AdminLiveAnchorVO> records = offset >= total
                ? List.of()
                : liveAnchorRepository.findAdminLiveAnchorPage(
                        offset,
                        pageSize,
                        platform,
                        anchorName,
                        roomId,
                        minFollowerCount,
                        maxFollowerCount);
        return new PageVO<>(pageNum, pageSize, total, records);
    }

    /**
     * 规范化平台筛选条件。
     *
     * @param platform 原始平台筛选条件
     * @return 数据库存储的平台编码
     */
    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return null;
        }
        String normalizedPlatform = platform.trim().toUpperCase(Locale.ROOT);
        return LivePlatform.fromCode(normalizedPlatform)
                .map(LivePlatform::getCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMETER_ERROR, "平台筛选值无效"));
    }

    /**
     * 规范化模糊查询文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本，空文本返回 null
     */
    private String normalizeLikeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 规范化精确查询文本。
     *
     * @param value 原始文本
     * @return 去除首尾空白后的文本，空文本返回 null
     */
    private String normalizeExactKeyword(String value) {
        return normalizeLikeKeyword(value);
    }

    /**
     * 校验关注人数区间。
     *
     * @param minFollowerCount 最小关注人数
     * @param maxFollowerCount 最大关注人数
     */
    private void validateFollowerRange(Long minFollowerCount, Long maxFollowerCount) {
        if (minFollowerCount != null && maxFollowerCount != null
                && minFollowerCount > maxFollowerCount) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "关注人数最小值不能大于最大值");
        }
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

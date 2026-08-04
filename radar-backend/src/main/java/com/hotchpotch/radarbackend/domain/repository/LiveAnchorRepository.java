package com.hotchpotch.radarbackend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.domain.mapper.LiveAnchorMapper;
import com.hotchpotch.radarbackend.vo.admin.AdminLiveAnchorVO;

/**
 * 直播主播数据访问仓库。
 */
@Repository
public class LiveAnchorRepository {

    /**
     * 直播主播 Mapper。
     */
    private final LiveAnchorMapper mapper;

    /**
     * 创建直播主播数据访问仓库。
     *
     * @param mapper 直播主播 Mapper
     */
    public LiveAnchorRepository(LiveAnchorMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按主键查询主播。
     *
     * @param id 主播主键
     * @return 主播实体，主播不存在时返回空 Optional
     */
    public Optional<LiveAnchor> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /**
     * 按平台和房间标识查询主播。
     *
     * @param platform 平台标识
     * @param roomId 平台直播间标识
     * @return 主播实体，主播不存在时返回空 Optional
     */
    public Optional<LiveAnchor> findByPlatformAndRoomId(String platform, String roomId) {
        return Optional.ofNullable(mapper.selectByPlatformAndRoomId(platform, roomId));
    }

    /**
     * 按主键批量查询主播。
     *
     * @param ids 主播主键集合，为空时直接返回空列表
     * @return 主播实体列表
     */
    public List<LiveAnchor> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(ids);
    }

    /**
     * 新增主播。
     *
     * @param entity 待新增的主播实体
     * @return 受影响的记录数
     */
    public int insert(LiveAnchor entity) {
        return mapper.insert(entity);
    }

    /**
     * 按主键更新主播。
     *
     * @param entity 待更新的主播实体，必须包含主键
     * @return 受影响的记录数
     */
    public int updateById(LiveAnchor entity) {
        return mapper.updateById(entity);
    }

    /**
     * 分页查询当前仍被用户关注的主播。
     *
     * @param offset 数据库查询偏移量
     * @param pageSize 每页记录数
     * @return 管理中心主播分页记录
     */
    public List<AdminLiveAnchorVO> findMonitoredLiveAnchorPage(long offset, int pageSize) {
        return mapper.selectMonitoredLiveAnchorPage(offset, pageSize);
    }

    /**
     * 统计当前仍被用户关注的主播数量。
     *
     * @return 当前监控主播数量
     */
    public long countMonitoredLiveAnchors() {
        return mapper.countMonitoredLiveAnchors();
    }
}

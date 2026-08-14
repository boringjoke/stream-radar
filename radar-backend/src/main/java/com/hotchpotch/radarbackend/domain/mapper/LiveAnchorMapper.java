package com.hotchpotch.radarbackend.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import com.hotchpotch.radarbackend.vo.admin.AdminAnchorStatisticRow;
import com.hotchpotch.radarbackend.vo.admin.AdminLiveAnchorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 直播主播数据访问 Mapper。
 */
@Mapper
public interface LiveAnchorMapper extends BaseMapper<LiveAnchor> {

    /**
     * 按平台和房间标识查询主播。
     *
     * @param platform 平台标识
     * @param roomId 平台直播间标识
     * @return 主播实体，不存在时返回 null
     */
    LiveAnchor selectByPlatformAndRoomId(
            @Param("platform") String platform,
            @Param("roomId") String roomId);

    /**
     * 分页查询全部主播及当前启用普通用户的关注人数。
     *
     * @param offset 数据库查询偏移量
     * @param pageSize 每页记录数
     * @return 管理中心主播分页记录
     */
    List<AdminLiveAnchorVO> selectAdminLiveAnchorPage(
            @Param("offset") long offset,
            @Param("pageSize") int pageSize,
            @Param("platform") String platform,
            @Param("anchorName") String anchorName,
            @Param("roomId") String roomId,
            @Param("minFollowerCount") Long minFollowerCount,
            @Param("maxFollowerCount") Long maxFollowerCount);

    /**
     * 统计符合筛选条件的全部主播数量。
     *
     * @return 符合筛选条件的主播数量
     */
    long countAdminLiveAnchors(
            @Param("platform") String platform,
            @Param("anchorName") String anchorName,
            @Param("roomId") String roomId,
            @Param("minFollowerCount") Long minFollowerCount,
            @Param("maxFollowerCount") Long maxFollowerCount);

    /**
     * 查询主播统计中间记录。
     *
     * @return 全部主播及其启用普通用户关注人数
     */
    List<AdminAnchorStatisticRow> selectAdminAnchorStatisticRows();

    /**
     * 查询当前仍被有效用户关注的去重主播，供监控任务使用。
     *
     * @return 去重后的监控主播列表
     */
    List<LiveAnchor> selectMonitoredLiveAnchors();
}

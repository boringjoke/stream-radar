package com.hotchpotch.radarbackend.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.radarbackend.domain.entity.LiveAnchor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}

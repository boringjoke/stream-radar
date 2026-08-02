package com.hotchpotch.radarbackend.domain.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户关注主播关系数据访问 Mapper。
 */
@Mapper
public interface UserFollowAnchorMapper extends BaseMapper<UserFollowAnchor> {

    /**
     * 查询用户与主播的指定关注关系。
     *
     * @param userId 用户主键
     * @param anchorId 主播主键
     * @return 关注关系，不存在时返回 null
     */
    UserFollowAnchor selectByUserIdAndAnchorId(
            @Param("userId") Long userId,
            @Param("anchorId") Long anchorId);

    /**
     * 查询用户的全部关注关系。
     *
     * @param userId 用户主键
     * @return 关注关系列表
     */
    List<UserFollowAnchor> selectByUserId(@Param("userId") Long userId);
}

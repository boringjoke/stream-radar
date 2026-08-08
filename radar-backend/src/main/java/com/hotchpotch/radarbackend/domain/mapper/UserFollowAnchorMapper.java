package com.hotchpotch.radarbackend.domain.mapper;

import java.util.List;
import java.util.Collection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO;
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
     * 按主播主键查询关注关系。
     *
     * @param anchorId 主播主键
     * @return 关注关系列表
     */
    List<UserFollowAnchor> selectByAnchorId(@Param("anchorId") Long anchorId);

    /**
     * 查询用户的全部关注关系。
     *
     * @param userId 用户主键
     * @return 关注关系列表
     */
    List<UserFollowAnchor> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询当前用户的主播卡片列表。
     *
     * @param userId 当前用户主键
     * @return 当前用户关注主播卡片列表
     */
    List<LiveAnchorCardVO> selectLiveAnchorCardsByUserId(@Param("userId") Long userId);

    /**
     * 统计当前用户关注关系数量。
     *
     * @param userId 当前用户主键
     * @return 关注关系数量
     */
    long countByUserId(@Param("userId") Long userId);

    /**
     * 按用户和关注关系主键删除单条关注关系。
     *
     * @param userId 当前用户主键
     * @param followId 关注关系主键
     * @return 受影响的记录数
     */
    int deleteByUserIdAndId(
            @Param("userId") Long userId,
            @Param("followId") Long followId);

    /**
     * 按用户和关注关系主键批量删除关注关系。
     *
     * @param userId 当前用户主键
     * @param followIds 关注关系主键列表
     * @return 受影响的记录数
     */
    int deleteByUserIdAndIds(
            @Param("userId") Long userId,
            @Param("followIds") Collection<Long> followIds);
}

package com.hotchpotch.radarbackend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.hotchpotch.radarbackend.domain.entity.UserFollowAnchor;
import com.hotchpotch.radarbackend.domain.mapper.UserFollowAnchorMapper;

/**
 * 用户关注主播关系数据访问仓库。
 */
@Repository
public class UserFollowAnchorRepository {

    /**
     * 用户关注主播关系 Mapper。
     */
    private final UserFollowAnchorMapper mapper;

    /**
     * 创建用户关注主播关系数据访问仓库。
     *
     * @param mapper 用户关注主播关系 Mapper
     */
    public UserFollowAnchorRepository(UserFollowAnchorMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按主键查询关注关系。
     *
     * @param id 关注关系主键
     * @return 关注关系，记录不存在时返回空 Optional
     */
    public Optional<UserFollowAnchor> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    /**
     * 按用户和主播查询指定关注关系。
     *
     * @param userId 用户主键
     * @param anchorId 主播主键
     * @return 关注关系，不存在时返回空 Optional
     */
    public Optional<UserFollowAnchor> findByUserIdAndAnchorId(Long userId, Long anchorId) {
        return Optional.ofNullable(mapper.selectByUserIdAndAnchorId(userId, anchorId));
    }

    /**
     * 查询主播当前的全部关注关系。
     *
     * @param anchorId 主播主键
     * @return 关注关系列表
     */
    public List<UserFollowAnchor> findByAnchorId(Long anchorId) {
        if (anchorId == null) {
            return List.of();
        }
        return mapper.selectByAnchorId(anchorId);
    }

    /**
     * 查询用户的全部关注关系。
     *
     * @param userId 用户主键
     * @return 用户关注关系列表
     */
    public List<UserFollowAnchor> findByUserId(Long userId) {
        return mapper.selectByUserId(userId);
    }

    /**
     * 按主键批量查询关注关系。
     *
     * @param ids 关注关系主键集合，为空时直接返回空列表
     * @return 关注关系列表
     */
    public List<UserFollowAnchor> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectByIds(ids);
    }

    /**
     * 查询当前用户关注关系数量。
     *
     * @param userId 当前用户主键
     * @return 关注关系数量
     */
    public long countByUserId(Long userId) {
        return mapper.countByUserId(userId);
    }

    /**
     * 查询当前用户的主播卡片列表。
     *
     * @param userId 当前用户主键
     * @return 主播卡片列表
     */
    public List<com.hotchpotch.radarbackend.vo.live.LiveAnchorCardVO> findLiveAnchorCardsByUserId(Long userId) {
        return mapper.selectLiveAnchorCardsByUserId(userId);
    }

    /**
     * 新增关注关系。
     *
     * @param entity 待新增的关注关系实体
     * @return 受影响的记录数
     */
    public int insert(UserFollowAnchor entity) {
        return mapper.insert(entity);
    }

    /**
     * 按主键删除关注关系。
     *
     * @param id 关注关系主键
     * @return 受影响的记录数
     */
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    /**
     * 按当前用户删除单条关注关系。
     *
     * @param userId 当前用户主键
     * @param followId 关注关系主键
     * @return 受影响的记录数
     */
    public int deleteByUserIdAndId(Long userId, Long followId) {
        return mapper.deleteByUserIdAndId(userId, followId);
    }

    /**
     * 按当前用户批量删除关注关系。
     *
     * @param userId 当前用户主键
     * @param followIds 关注关系主键集合
     * @return 受影响的记录数
     */
    public int deleteByUserIdAndIds(Long userId, Collection<Long> followIds) {
        if (followIds == null || followIds.isEmpty()) {
            return 0;
        }
        return mapper.deleteByUserIdAndIds(userId, followIds);
    }
}

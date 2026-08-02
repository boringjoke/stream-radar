package com.hotchpotch.radarbackend.domain.repository;

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
     * 查询用户的全部关注关系。
     *
     * @param userId 用户主键
     * @return 用户关注关系列表
     */
    public List<UserFollowAnchor> findByUserId(Long userId) {
        return mapper.selectByUserId(userId);
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
}

package com.hotchpotch.radarbackend.service.admin;

import java.net.URI;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import com.hotchpotch.radarbackend.config.GuestLiveDemoProperties;
import com.hotchpotch.radarbackend.domain.enums.LivePlatform;
import com.hotchpotch.radarbackend.domain.repository.LiveAnchorRepository;
import com.hotchpotch.radarbackend.domain.repository.SysUserRepository;
import com.hotchpotch.radarbackend.vo.admin.AdminAnchorStatisticRow;
import com.hotchpotch.radarbackend.vo.admin.AdminOverviewVO;
import org.springframework.stereotype.Service;

/**
 * 管理中心概览统计业务服务。
 */
@Service
public class AdminOverviewService {

    /**
     * 系统用户数据访问仓库。
     */
    private final SysUserRepository sysUserRepository;

    /**
     * 直播主播数据访问仓库。
     */
    private final LiveAnchorRepository liveAnchorRepository;

    /**
     * 游客首页演示主播配置。
     */
    private final GuestLiveDemoProperties guestLiveDemoProperties;

    /**
     * 创建管理中心概览统计业务服务。
     *
     * @param sysUserRepository 系统用户数据访问仓库
     * @param liveAnchorRepository 直播主播数据访问仓库
     * @param guestLiveDemoProperties 游客首页演示主播配置
     */
    public AdminOverviewService(
            SysUserRepository sysUserRepository,
            LiveAnchorRepository liveAnchorRepository,
            GuestLiveDemoProperties guestLiveDemoProperties) {
        this.sysUserRepository = sysUserRepository;
        this.liveAnchorRepository = liveAnchorRepository;
        this.guestLiveDemoProperties = guestLiveDemoProperties;
    }

    /**
     * 查询管理中心全平台概览统计。
     *
     * @return 管理中心概览统计
     */
    public AdminOverviewVO overview() {
        long userCount = sysUserRepository.countEnabledNormalUsers();
        List<AdminAnchorStatisticRow> rows = liveAnchorRepository.findAdminAnchorStatisticRows();
        Set<String> demoKeys = guestDemoKeys();
        Map<LivePlatform, Long> platformCounts = new EnumMap<>(LivePlatform.class);
        for (LivePlatform platform : LivePlatform.values()) {
            platformCounts.put(platform, 0L);
        }

        long anchorCount = 0;
        long followedAnchorCount = 0;
        for (AdminAnchorStatisticRow row : rows) {
            long followerCount = row.getFollowerCount() == null ? 0 : row.getFollowerCount();
            boolean followedByNormalUser = followerCount > 0;
            if (followedByNormalUser) {
                followedAnchorCount++;
            }

            boolean guestDemo = demoKeys.contains(toAnchorKey(row.getPlatform(), row.getRoomId()));
            if (guestDemo && !followedByNormalUser) {
                continue;
            }

            anchorCount++;
            LivePlatform.fromCode(row.getPlatform()).ifPresent(platform ->
                    platformCounts.compute(platform, (key, count) -> count == null ? 1L : count + 1L));
        }

        Map<String, Long> platformAnchorCounts = new LinkedHashMap<>();
        for (LivePlatform platform : LivePlatform.values()) {
            platformAnchorCounts.put(platform.getCode(), platformCounts.get(platform));
        }
        return new AdminOverviewVO(
                userCount,
                anchorCount,
                followedAnchorCount,
                platformAnchorCounts);
    }

    /**
     * 获取当前游客首页演示主播的唯一标识集合。
     *
     * @return 平台和房间号组合集合
     */
    private Set<String> guestDemoKeys() {
        Set<String> keys = new HashSet<>();
        addDemoKey(keys, LivePlatform.BILIBILI, guestLiveDemoProperties.getBilibiliUrl(), "22637261");
        addDemoKey(keys, LivePlatform.DOUYU, guestLiveDemoProperties.getDouyuUrl(), "9999");
        addDemoKey(keys, LivePlatform.HUYA, guestLiveDemoProperties.getHuyaUrl(), "998");
        addDemoKey(keys, LivePlatform.DOUYIN, guestLiveDemoProperties.getDouyinUrl(), "690434662");
        return keys;
    }

    /**
     * 从演示主播 URL 提取房间号并加入统计排除集合。
     *
     * @param keys 统计排除集合
     * @param platform 平台
     * @param url 演示主播 URL
     * @param fallbackRoomId 配置异常时的房间号
     */
    private void addDemoKey(Set<String> keys, LivePlatform platform, String url, String fallbackRoomId) {
        String roomId = extractLastPathSegment(url);
        keys.add(toAnchorKey(platform.getCode(), roomId == null ? fallbackRoomId : roomId));
    }

    /**
     * 提取 URL 路径最后一个非空片段。
     *
     * @param url 原始 URL
     * @return 房间号片段，无法提取时返回 null
     */
    private String extractLastPathSegment(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String path = URI.create(url.trim()).getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] segments = path.split("/");
            for (int index = segments.length - 1; index >= 0; index--) {
                if (!segments[index].isBlank()) {
                    return segments[index];
                }
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    /**
     * 生成平台和房间号组合键。
     *
     * @param platform 平台
     * @param roomId 房间号
     * @return 组合键
     */
    private String toAnchorKey(String platform, String roomId) {
        return String.valueOf(platform) + "|" + String.valueOf(roomId);
    }
}

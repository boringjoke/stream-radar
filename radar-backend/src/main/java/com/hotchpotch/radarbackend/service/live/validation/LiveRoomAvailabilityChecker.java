package com.hotchpotch.radarbackend.service.live.validation;

import com.hotchpotch.radarbackend.service.live.url.ResolvedLiveRoom;

/**
 * 直播间真实存在性校验扩展点。
 *
 * <p>当前阶段不注册平台实现，因此关注流程只执行 URL 安全解析。后续接入正式数据源
 * 后，由平台校验实现返回 {@link LiveRoomCheckStatus#NOT_FOUND}，关注关系将在保存前被拒绝。</p>
 */
public interface LiveRoomAvailabilityChecker {

    /**
     * 判断当前校验器是否负责指定直播间平台。
     *
     * @param room URL 解析结果
     * @return 是否支持该平台
     */
    boolean supports(ResolvedLiveRoom room);

    /**
     * 校验直播间是否真实存在。
     *
     * @param room URL 解析结果
     * @return 平台校验结果
     */
    LiveRoomCheckResult check(ResolvedLiveRoom room);
}

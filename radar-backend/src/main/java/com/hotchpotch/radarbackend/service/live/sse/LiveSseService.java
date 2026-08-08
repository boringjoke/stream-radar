package com.hotchpotch.radarbackend.service.live.sse;

import com.hotchpotch.radarbackend.common.exception.BusinessException;
import com.hotchpotch.radarbackend.common.exception.ErrorCode;
import com.hotchpotch.radarbackend.security.RadarUserPrincipal;
import com.hotchpotch.radarbackend.service.live.LiveService;
import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 当前用户直播 SSE 连接业务服务。
 */
@Service
public class LiveSseService {

    /**
     * 直播首页查询服务。
     */
    private final LiveService liveService;

    /**
     * SSE 进程内连接注册表。
     */
    private final LiveSseConnectionRegistry connectionRegistry;

    /**
     * 创建直播 SSE 服务。
     *
     * @param liveService 直播首页查询服务
     * @param connectionRegistry SSE 连接注册表
     */
    public LiveSseService(
            LiveService liveService,
            LiveSseConnectionRegistry connectionRegistry) {
        this.liveService = liveService;
        this.connectionRegistry = connectionRegistry;
    }

    /**
     * 创建当前用户的 SSE 连接并发送初始快照。
     *
     * @param authentication 当前认证对象
     * @return SSE 发射器
     */
    public SseEmitter connect(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        LiveHomeVO home = liveService.getHome(authentication);
        return connectionRegistry.connect(userId, home);
    }

    /**
     * 从当前认证主体取得用户主键。
     *
     * @param authentication 当前认证对象
     * @return 当前用户主键
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RadarUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}

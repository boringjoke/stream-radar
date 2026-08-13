package com.hotchpotch.radarbackend.service.live.guest;

import com.hotchpotch.radarbackend.vo.live.LiveHomeVO;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 游客首页真实数据 SSE 服务。
 */
@Service
public class GuestLiveSseService {

    /**
     * 游客首页真实数据服务。
     */
    private final GuestLiveHomeService guestLiveHomeService;

    /**
     * 游客 SSE 连接注册表。
     */
    private final GuestLiveSseConnectionRegistry connectionRegistry;

    /**
     * 创建游客首页 SSE 服务。
     *
     * @param guestLiveHomeService 游客首页真实数据服务
     * @param connectionRegistry 游客 SSE 连接注册表
     */
    public GuestLiveSseService(
            GuestLiveHomeService guestLiveHomeService,
            GuestLiveSseConnectionRegistry connectionRegistry) {
        this.guestLiveHomeService = guestLiveHomeService;
        this.connectionRegistry = connectionRegistry;
    }

    /**
     * 创建游客首页 SSE 连接并发送初始快照。
     *
     * @return SSE 发射器
     */
    public SseEmitter connect() {
        LiveHomeVO home = guestLiveHomeService.getHome();
        return connectionRegistry.connect(home);
    }
}

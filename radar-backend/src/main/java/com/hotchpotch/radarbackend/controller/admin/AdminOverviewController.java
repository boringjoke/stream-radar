package com.hotchpotch.radarbackend.controller.admin;

import com.hotchpotch.radarbackend.common.response.ApiResponse;
import com.hotchpotch.radarbackend.service.admin.AdminOverviewService;
import com.hotchpotch.radarbackend.vo.admin.AdminOverviewVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理中心概览统计接口。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminOverviewController {

    /**
     * 管理中心概览统计业务服务。
     */
    private final AdminOverviewService adminOverviewService;

    /**
     * 创建管理中心概览统计控制器。
     *
     * @param adminOverviewService 管理中心概览统计业务服务
     */
    public AdminOverviewController(AdminOverviewService adminOverviewService) {
        this.adminOverviewService = adminOverviewService;
    }

    /**
     * 查询全平台总览和平台拆分统计。
     *
     * @return 管理中心概览统计
     */
    @GetMapping("/overview")
    public ApiResponse<AdminOverviewVO> overview() {
        return ApiResponse.success(adminOverviewService.overview());
    }
}

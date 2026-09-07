package com.mytransitgps.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 大屏静态页面入口控制器。
 *
 * <p>将 {@code /dashboard} 转发到现有前端页面，不承担数据查询或采集业务。
 */
@Controller
public class DashboardPageController {
    @GetMapping({"/dashboard", "/dashboard/", "/dashboard/ciq", "/dashboard/ciq/", "/dashboard/ciqbus", "/dashboard/ciqbus/"})
    public String dashboard() {
        return "forward:/dashboard/index.html";
    }
}

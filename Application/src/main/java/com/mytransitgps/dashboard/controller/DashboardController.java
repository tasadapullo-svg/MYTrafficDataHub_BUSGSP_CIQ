package com.mytransitgps.dashboard.controller;

import com.mytransitgps.dashboard.dto.DashboardDtos;
import com.mytransitgps.dashboard.service.DashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * BUS GPS 现有监测大屏 REST 控制器。
 *
 * <p>提供城市、车辆、轨迹、采集状态、异常和系统状态等只读接口；CIQ 后续通过独立 Provider 扩展，
 * 本控制器现有接口和返回语义保持兼容。
 */
@Validated
@RestController
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/cities")
    public List<DashboardDtos.City> cities() { return service.cities(); }

    @GetMapping("/vehicles")
    public DashboardDtos.Vehicles vehicles(@RequestParam(defaultValue = "JB") String city) {
        return service.vehicles(city);
    }

    @GetMapping("/route-traces")
    public DashboardDtos.RouteTraces routeTraces(@RequestParam(defaultValue = "JB") String city,
                                                  @RequestParam(required = false) String route,
                                                  @RequestParam(defaultValue = "false") boolean all) {
        return service.routeTraces(city, route, all);
    }

    @GetMapping("/acquisition")
    public List<DashboardDtos.Acquisition> acquisition() { return service.acquisition(); }

    @GetMapping("/acquisition/trend")
    public List<DashboardDtos.TrendPoint> trend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.trend(date);
    }

    @GetMapping("/overview")
    public DashboardDtos.Overview overview() { return service.overview(); }

    @GetMapping("/anomalies/recent")
    public DashboardDtos.Availability<List<DashboardDtos.Anomaly>> anomalies(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.recentAnomalies(limit);
    }

    @GetMapping("/api-requests/recent")
    public DashboardDtos.Availability<List<DashboardDtos.ApiRequest>> apiRequests(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return service.recentApiRequests(limit);
    }

    @GetMapping("/system-status")
    public DashboardDtos.SystemStatus systemStatus() { return service.systemStatus(); }


    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest(IllegalArgumentException ex) {
        return Map.of("error", "INVALID_REQUEST", "message", ex.getMessage());
    }
}

package com.mytransitgps.dashboard.controller;

import com.mytransitgps.modules.ciqbus.dashboard.dto.CiqBusDashboardDtos;
import com.mytransitgps.modules.ciqbus.dashboard.service.CiqBusDashboardService;
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

/** CIQBus 大屏 REST 只读接口。 */
@Validated
@RestController
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
@RequestMapping("/api/ciqbus")
public class CiqBusDashboardController {
    private final CiqBusDashboardService service;

    public CiqBusDashboardController(CiqBusDashboardService service) {
        this.service = service;
    }

    @GetMapping("/realtime")
    public CiqBusDashboardDtos.RealtimeResponse realtime() { return service.realtime(); }

    @GetMapping("/overview")
    public CiqBusDashboardDtos.Overview overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.overview(date);
    }

    @GetMapping("/hourly")
    public List<CiqBusDashboardDtos.HourlyPoint> hourly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.hourly(date);
    }

    @GetMapping("/passages")
    public CiqBusDashboardDtos.Page<CiqBusDashboardDtos.Passage> passages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "ALL") String route,
            @RequestParam(defaultValue = "ALL") String direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.passages(date, route, direction, page, size);
    }

    @GetMapping("/crossings")
    public CiqBusDashboardDtos.Page<CiqBusDashboardDtos.Crossing> crossings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "ALL") String route,
            @RequestParam(defaultValue = "ALL") String direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.crossings(date, route, direction, page, size);
    }

    @GetMapping("/status")
    public CiqBusDashboardDtos.Status status() { return service.status(); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest(IllegalArgumentException ex) {
        return Map.of("error", "INVALID_REQUEST", "message", ex.getMessage());
    }
}

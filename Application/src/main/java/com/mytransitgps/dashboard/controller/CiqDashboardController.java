package com.mytransitgps.dashboard.controller;

import com.mytransitgps.modules.ciq.dashboard.dto.CiqDashboardDtos;
import com.mytransitgps.modules.ciq.dashboard.service.CiqDashboardService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Malaysia CIQ 只读大屏接口。 */
@RestController
@ConditionalOnProperty(prefix="mytransitgps.database",name="enabled",havingValue="true")
@RequestMapping("/api/ciq/dashboard")
public class CiqDashboardController {
    private final CiqDashboardService service;
    public CiqDashboardController(CiqDashboardService service){this.service=service;}
    @GetMapping("/overview") public CiqDashboardDtos.Overview overview(){return service.overview();}
    @GetMapping("/apis") public List<CiqDashboardDtos.ApiStatus> apis(){return service.apis();}
    @GetMapping("/trend") public List<CiqDashboardDtos.TrendPoint> trend(@RequestParam(defaultValue="3") @Min(3) @Max(720) int hours){return service.trend(hours);}
    @GetMapping("/audit") public List<CiqDashboardDtos.AuditRow> audit(){return service.audit();}
    @GetMapping("/requests/recent") public List<CiqDashboardDtos.RequestLog> requests(@RequestParam(defaultValue="20") @Min(1) @Max(50) int limit){return service.requests(limit);}
    @GetMapping("/storage") public CiqDashboardDtos.Storage storage(){return service.storage();}
    @GetMapping("/map-points") public List<CiqDashboardDtos.MapPoint> mapPoints(@RequestParam(defaultValue="ALL") String api,@RequestParam(defaultValue="1200") @Min(1) @Max(3000) int limit){return service.mapPoints(api,limit);}
    @GetMapping("/data/latest") public List<CiqDashboardDtos.DataRecord> dataRecords(
            @RequestParam(defaultValue="ALL") String api,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required=false) String category,
            @RequestParam(defaultValue="60") @Min(1) @Max(120) int limit){return service.dataRecords(api,from,to,category,limit);}
    @GetMapping("/api/{apiCode}") public CiqDashboardDtos.ApiStatus api(@PathVariable String apiCode){return service.api(apiCode);}
    @GetMapping public CiqDashboardDtos.DashboardPayload payload(@RequestParam(defaultValue="3") int trendHours){return service.payload(trendHours);}
}

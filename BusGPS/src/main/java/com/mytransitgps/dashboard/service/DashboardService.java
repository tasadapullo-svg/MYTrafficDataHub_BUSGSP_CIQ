package com.mytransitgps.dashboard.service;

import com.mytransitgps.dashboard.dto.DashboardDtos;
import com.mytransitgps.dashboard.model.DashboardCity;
import com.mytransitgps.dashboard.repository.DashboardRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
@Transactional(readOnly = true, timeout = 10)
/**
 * BUS GPS 大屏应用服务。
 *
 * <p>负责组合 Repository 查询结果并形成前端 DTO，不参与车辆采集、QC 或数据库写入。
 * CIQ 监控通过模块 Provider 独立扩展。
 */
public class DashboardService {
    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    public List<DashboardDtos.City> cities() {
        return Arrays.stream(DashboardCity.values()).map(city -> new DashboardDtos.City(city.code(),
                city.nameEn(), city.nameZh(), city.latitude(), city.longitude(), city.zoom())).toList();
    }

    public DashboardDtos.Vehicles vehicles(String cityCode) {
        DashboardCity city = DashboardCity.fromCode(cityCode);
        List<DashboardDtos.Vehicle> vehicles = repository.vehicles(city);
        return new DashboardDtos.Vehicles(city.code(), Instant.now(), vehicles.size(), vehicles);
    }

    public DashboardDtos.RouteTraces routeTraces(String cityCode, String routeKey, boolean all) {
        DashboardCity city = DashboardCity.fromCode(cityCode);
        return all ? repository.allRouteTraces(city) : repository.routeTraces(city, routeKey);
    }

    public List<DashboardDtos.Acquisition> acquisition() {
        return Arrays.stream(DashboardCity.values()).map(repository::acquisition).toList();
    }

    public List<DashboardDtos.TrendPoint> trend(LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now(DashboardRepository.MALAYSIA_ZONE) : date;
        return Arrays.stream(DashboardCity.values()).flatMap(city -> repository.trend(city, effectiveDate).stream()).toList();
    }

    public DashboardDtos.Overview overview() {
        List<DashboardDtos.Acquisition> acquisitions = acquisition();
        long active = Arrays.stream(DashboardCity.values()).mapToLong(repository::latestVehicleCount).sum();
        long today = acquisitions.stream().mapToLong(DashboardDtos.Acquisition::todayRecords).sum();
        long yesterday = acquisitions.stream().mapToLong(DashboardDtos.Acquisition::yesterdayRecords).sum();
        long total = Arrays.stream(DashboardCity.values()).mapToLong(repository::observationCount).sum();
        long abnormal = Arrays.stream(DashboardCity.values()).mapToLong(repository::abnormalVehicleCount).sum();
        Instant latest = latestGps();
        long requests = acquisitions.stream().mapToLong(DashboardDtos.Acquisition::requestCount).sum();
        long successes = acquisitions.stream().mapToLong(DashboardDtos.Acquisition::successfulRequests).sum();
        Double successRate = requests == 0 ? null : successes * 100.0 / requests;
        return new DashboardDtos.Overview(active, today, yesterday, total, false, latest, abnormal,
                successRate, "Eligible latest-state vehicles seen within the last 10 minutes",
                "Distinct vehicles with persisted QC events in the last 1 hour", Instant.now());
    }

    public DashboardDtos.Availability<List<DashboardDtos.Anomaly>> recentAnomalies(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<DashboardDtos.Anomaly> data = Arrays.stream(DashboardCity.values())
                .flatMap(city -> repository.anomalies(city, safeLimit).stream())
                .sorted(Comparator.comparing(DashboardDtos.Anomaly::time,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit).toList();
        return new DashboardDtos.Availability<>(true, data, null);
    }

    public DashboardDtos.Availability<List<DashboardDtos.ApiRequest>> recentApiRequests(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<DashboardDtos.ApiRequest> data = Arrays.stream(DashboardCity.values())
                .flatMap(city -> repository.apiRequests(city, safeLimit).stream())
                .sorted(Comparator.comparing(DashboardDtos.ApiRequest::time,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit).toList();
        return new DashboardDtos.Availability<>(true, data, null);
    }

    public DashboardDtos.SystemStatus systemStatus() {
        Instant checkedAt = Instant.now();
        try {
            Instant latest = latestGps();
            long age = latest == null ? Long.MAX_VALUE : Math.max(0, Duration.between(latest, checkedAt).getSeconds());
            String status = latest == null || age > 600 ? "WARNING" : "NORMAL";
            String message = latest == null ? "No latest GPS data" : age > 600 ? "GPS data is stale" : "System normal";
            return new DashboardDtos.SystemStatus(status, true, latest, age, message, checkedAt);
        } catch (DataAccessException ex) {
            return new DashboardDtos.SystemStatus("ERROR", false, null, Long.MAX_VALUE,
                    "Dashboard database unavailable", checkedAt);
        }
    }

    private Instant latestGps() {
        return Arrays.stream(DashboardCity.values()).map(repository::latestGps).filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
    }
}

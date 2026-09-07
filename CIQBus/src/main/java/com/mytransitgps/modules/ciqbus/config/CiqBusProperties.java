package com.mytransitgps.modules.ciqbus.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ciqbus")
public class CiqBusProperties {
    private String timezone = "Asia/Kuala_Lumpur";
    private final ManualTest manualTest = new ManualTest();
    private final Scheduler scheduler = new Scheduler();
    private final Storage storage = new Storage();
    private final Http http = new Http();
    private final Lta lta = new Lta();
    private final Redis redis = new Redis();
    private final Matching matching = new Matching();
    private final DailyExport dailyExport = new DailyExport();
    private List<RouteStop> routeStops = List.of();

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Storage getStorage() {
        return storage;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Http getHttp() {
        return http;
    }

    public Lta getLta() {
        return lta;
    }

    public Redis getRedis() {
        return redis;
    }

    public Matching getMatching() {
        return matching;
    }

    public DailyExport getDailyExport() {
        return dailyExport;
    }

    public ManualTest getManualTest() {
        return manualTest;
    }

    public List<RouteStop> getRouteStops() {
        return routeStops;
    }

    public void setRouteStops(List<RouteStop> routeStops) {
        this.routeStops = List.copyOf(routeStops);
    }

    public boolean isLtaAccountKeyConfigured() {
        return lta.accountKey != null && !lta.accountKey.isBlank();
    }

    public static class Storage {
        private String root = "C:/Users/DELL/Desktop/马来西亚_新加坡数据综合代码采集项目_BUS_CIQ_20260831_代码数据资料/MYTrafficDataHub_20260829/data_download/CIQBus";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class ManualTest {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Scheduler {
        private boolean enabled;
        private String cron = "0 */2 * * * *";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Http {
        private int connectTimeoutSeconds = 10;
        private int requestTimeoutSeconds = 30;

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getRequestTimeoutSeconds() {
            return requestTimeoutSeconds;
        }

        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
            this.requestTimeoutSeconds = requestTimeoutSeconds;
        }

    }

    public static class Lta {
        private String baseUrl = "https://datamall2.mytransport.sg/ltaodataservice";
        private String accountKey = "";
        private String accept = "application/json";
        private List<String> stopCodes = List.of("46219", "46211", "46109", "46101");
        private List<String> targetServices = List.of("160", "170", "170X", "950");

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAccountKey() {
            return accountKey;
        }

        public void setAccountKey(String accountKey) {
            this.accountKey = accountKey;
        }

        public String getAccept() {
            return accept;
        }

        public void setAccept(String accept) {
            this.accept = accept;
        }

        public List<String> getStopCodes() {
            return stopCodes;
        }

        public void setStopCodes(List<String> stopCodes) {
            this.stopCodes = List.copyOf(stopCodes);
        }

        public List<String> getTargetServices() {
            return targetServices;
        }

        public void setTargetServices(List<String> targetServices) {
            this.targetServices = List.copyOf(targetServices);
        }
    }

    public static class Redis {
        private long activeTtlMinutes = 90;
        private long passageTtlMinutes = 240;
        private long completedTtlMinutes = 240;

        public long getActiveTtlMinutes() { return activeTtlMinutes; }
        public void setActiveTtlMinutes(long activeTtlMinutes) { this.activeTtlMinutes = activeTtlMinutes; }
        public long getPassageTtlMinutes() { return passageTtlMinutes; }
        public void setPassageTtlMinutes(long passageTtlMinutes) { this.passageTtlMinutes = passageTtlMinutes; }
        public long getCompletedTtlMinutes() { return completedTtlMinutes; }
        public void setCompletedTtlMinutes(long completedTtlMinutes) { this.completedTtlMinutes = completedTtlMinutes; }
    }

    public static class Matching {
        private double threshold = 60;
        private long maxCandidateAgeMinutes = 90;
        private double maxGpsJumpKmh = 90;
        private double nearStopMeters = 450;
        private long etaZeroSeconds = 90;

        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
        public long getMaxCandidateAgeMinutes() { return maxCandidateAgeMinutes; }
        public void setMaxCandidateAgeMinutes(long maxCandidateAgeMinutes) { this.maxCandidateAgeMinutes = maxCandidateAgeMinutes; }
        public double getMaxGpsJumpKmh() { return maxGpsJumpKmh; }
        public void setMaxGpsJumpKmh(double maxGpsJumpKmh) { this.maxGpsJumpKmh = maxGpsJumpKmh; }
        public double getNearStopMeters() { return nearStopMeters; }
        public void setNearStopMeters(double nearStopMeters) { this.nearStopMeters = nearStopMeters; }
        public long getEtaZeroSeconds() { return etaZeroSeconds; }
        public void setEtaZeroSeconds(long etaZeroSeconds) { this.etaZeroSeconds = etaZeroSeconds; }
    }

    public static class DailyExport {
        private String cron = "0 10 0 * * *";

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    public static class RouteStop {
        private String routeNo;
        private String ltaDirection;
        private String directionCode;
        private String stopCode;
        private String stopName;
        private Integer stopSequence;
        private String passageType;
        private Double latitude;
        private Double longitude;

        public String getRouteNo() { return routeNo; }
        public void setRouteNo(String routeNo) { this.routeNo = routeNo; }
        public String getLtaDirection() { return ltaDirection; }
        public void setLtaDirection(String ltaDirection) { this.ltaDirection = ltaDirection; }
        public String getDirectionCode() { return directionCode; }
        public void setDirectionCode(String directionCode) { this.directionCode = directionCode; }
        public String getStopCode() { return stopCode; }
        public void setStopCode(String stopCode) { this.stopCode = stopCode; }
        public String getStopName() { return stopName; }
        public void setStopName(String stopName) { this.stopName = stopName; }
        public Integer getStopSequence() { return stopSequence; }
        public void setStopSequence(Integer stopSequence) { this.stopSequence = stopSequence; }
        public String getPassageType() { return passageType; }
        public void setPassageType(String passageType) { this.passageType = passageType; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }
}

package com.mytransitgps.modules.ciq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** CIQ 八接口统一配置。密钥仅用于请求头，日志中禁止输出。 */
@ConfigurationProperties(prefix = "traffic.ciq")
public class CiqProperties {
    private boolean enabled;
    private String timezone = "Asia/Kuala_Lumpur";
    private final Schedule schedule = new Schedule();
    private final Http http = new Http();
    private final Lta lta = new Lta();
    private final Collectors collectors = new Collectors();
    private final Persistence persistence = new Persistence();
    private final Storage storage = new Storage();
    private final Archive archive = new Archive();
    private final ManualTest manualTest = new ManualTest();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Schedule getSchedule() { return schedule; }
    public Http getHttp() { return http; }
    public Lta getLta() { return lta; }
    public Collectors getCollectors() { return collectors; }
    public Persistence getPersistence() { return persistence; }
    public Storage getStorage() { return storage; }
    public Archive getArchive() { return archive; }
    public ManualTest getManualTest() { return manualTest; }
    public boolean isAccountKeyConfigured() { return lta.accountKey != null && !lta.accountKey.isBlank(); }

    public static class Schedule {
        private boolean enabled;
        private String cron = "0 0,15,30,45 * * * *"; // legacy API01 compatibility
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }
    public static class Http {
        private int connectTimeoutSeconds = 10;
        private int requestTimeoutSeconds = 30;
        private int retryCount = 3;
        private int retryDelaySeconds = 5;
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int v) { connectTimeoutSeconds=v; }
        public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
        public void setRequestTimeoutSeconds(int v) { requestTimeoutSeconds=v; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int v) { retryCount=v; }
        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(int v) { retryDelaySeconds=v; }
    }
    public static class Lta {
        private String baseUrl; private String accountKey=""; private String accept="application/json";
        public String getBaseUrl(){return baseUrl;} public void setBaseUrl(String v){baseUrl=v;}
        public String getAccountKey(){return accountKey;} public void setAccountKey(String v){accountKey=v;}
        public String getAccept(){return accept;} public void setAccept(String v){accept=v;}
    }
    public static class Collectors {
        private final TrafficSpeed trafficSpeed = new TrafficSpeed();
        private final CollectorSettings estimatedTravelTimes = new CollectorSettings("/EstTravelTimes", "0 0,15,30,45 * * * *");
        private final CollectorSettings trafficIncidents = new CollectorSettings("/TrafficIncidents", "0 */5 * * * *");
        private final CollectorSettings vms = new CollectorSettings("/VMS", "0 */5 * * * *");
        private final CollectorSettings faultyTrafficLights = new CollectorSettings("/FaultyTrafficLights", "0 0 * * * *");
        private final CollectorSettings roadWorks = new CollectorSettings("/RoadWorks", "0 20 0 * * *");
        private final CollectorSettings trafficFlow = new CollectorSettings("/TrafficFlow", "0 40 0 L * *");
        private final CollectorSettings roadOpenings = new CollectorSettings("/RoadOpenings", "0 25 0 * * *");
        public TrafficSpeed getTrafficSpeed(){return trafficSpeed;}
        public CollectorSettings getEstimatedTravelTimes(){return estimatedTravelTimes;}
        public CollectorSettings getTrafficIncidents(){return trafficIncidents;}
        public CollectorSettings getVms(){return vms;}
        public CollectorSettings getFaultyTrafficLights(){return faultyTrafficLights;}
        public CollectorSettings getRoadWorks(){return roadWorks;}
        public CollectorSettings getTrafficFlow(){return trafficFlow;}
        public CollectorSettings getRoadOpenings(){return roadOpenings;}
    }
    public static class CollectorSettings {
        private boolean enabled; private String endpoint; private String cron; private int pageSize=500; private int maxPages=1000;
        public CollectorSettings() {}
        public CollectorSettings(String endpoint,String cron){this.endpoint=endpoint;this.cron=cron;}
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
        public String getEndpoint(){return endpoint;} public void setEndpoint(String v){endpoint=v;}
        public String getCron(){return cron;} public void setCron(String v){cron=v;}
        public int getPageSize(){return pageSize;} public void setPageSize(int v){pageSize=v;}
        public int getMaxPages(){return maxPages;} public void setMaxPages(int v){maxPages=v;}
    }
    public static class TrafficSpeed extends CollectorSettings {
        public TrafficSpeed(){ super("/v4/TrafficSpeedBands", "0 0,15,30,45 * * * *"); }
    }
    public static class Persistence {
        private int batchSize=500; private boolean databaseWriteEnabled;
        public int getBatchSize(){return batchSize;} public void setBatchSize(int v){batchSize=v;}
        public boolean isDatabaseWriteEnabled(){return databaseWriteEnabled;} public void setDatabaseWriteEnabled(boolean v){databaseWriteEnabled=v;}
    }
    public static class Storage {
        private String rootDirectory; public String getRootDirectory(){return rootDirectory;} public void setRootDirectory(String v){rootDirectory=v;}
    }
    public static class Archive {
        private boolean enabled; private String cron="0 30 0 * * *"; private String zone="Asia/Kuala_Lumpur";
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
        public String getCron(){return cron;} public void setCron(String v){cron=v;}
        public String getZone(){return zone;} public void setZone(String v){zone=v;}
    }
    public static class ManualTest {
        private boolean enabled; public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
    }
}

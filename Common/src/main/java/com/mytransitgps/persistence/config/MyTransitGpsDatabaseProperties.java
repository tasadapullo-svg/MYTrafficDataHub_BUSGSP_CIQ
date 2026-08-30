package com.mytransitgps.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 中文名称：MYTransitGPS 数据库业务配置。
 *
 * 功能说明：集中承载数据库模块、批量持久化和显式数据库验收测试的配置。
 * 十分钟数据库采集默认关闭，普通应用启动不会自动发起真实 GTFS 请求。
 */
@ConfigurationProperties(prefix = "mytransitgps")
public class MyTransitGpsDatabaseProperties {

    public final Database database = new Database();
    public final Persistence persistence = new Persistence();
    public final DbTest dbTest = new DbTest();
    public final LongTest longTest = new LongTest();
    public final MulticityDbTest multicityDbTest = new MulticityDbTest();
    public final Continuous continuous = new Continuous();
    private String workspaceRoot;

    public Database getDatabase() { return database; }
    public Persistence getPersistence() { return persistence; }
    public DbTest getDbTest() { return dbTest; }
    public LongTest getLongTest() { return longTest; }
    public MulticityDbTest getMulticityDbTest() { return multicityDbTest; }
    public Continuous getContinuous() { return continuous; }
    public String getWorkspaceRoot() { return workspaceRoot; }
    public void setWorkspaceRoot(String workspaceRoot) { this.workspaceRoot = workspaceRoot; }

    /** 数据库总开关及默认 Schema/Feed 配置。 */
    public static class Database {
        private boolean enabled;
        private String schema = "jb";
        private String feedId = "mybas-johor";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public String getFeedId() { return feedId; }
        public void setFeedId(String feedId) { this.feedId = feedId; }
    }

    /** 批量写入、QC 明细和 Latest State 的持久化参数。 */
    public static class Persistence {
        private int batchSize = 200;
        private int qcBatchSize = 500;
        private int staticBatchSize = 500;
        private boolean saveRealtimeEntity = true;
        private boolean saveQcDetail = true;
        private boolean updateLatestState = true;
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getQcBatchSize() { return qcBatchSize; }
        public void setQcBatchSize(int qcBatchSize) { this.qcBatchSize = qcBatchSize; }
        public int getStaticBatchSize() { return staticBatchSize; }
        public void setStaticBatchSize(int staticBatchSize) { this.staticBatchSize = staticBatchSize; }
        public boolean isSaveRealtimeEntity() { return saveRealtimeEntity; }
        public void setSaveRealtimeEntity(boolean saveRealtimeEntity) { this.saveRealtimeEntity = saveRealtimeEntity; }
        public boolean isSaveQcDetail() { return saveQcDetail; }
        public void setSaveQcDetail(boolean saveQcDetail) { this.saveQcDetail = saveQcDetail; }
        public boolean isUpdateLatestState() { return updateLatestState; }
        public void setUpdateLatestState(boolean updateLatestState) { this.updateLatestState = updateLatestState; }
    }

    /** 单城市十分钟数据库测试参数。 */
    public static class DbTest {
        private boolean enabled;
        private String feedId = "mybas-johor";
        private int durationMinutes = 10;
        private int cycles = 5;
        private int intervalSeconds = 120;
        private boolean validateAfterRun = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedId() { return feedId; }
        public void setFeedId(String feedId) { this.feedId = feedId; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public int getCycles() { return cycles; }
        public void setCycles(int cycles) { this.cycles = cycles; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isValidateAfterRun() { return validateAfterRun; }
        public void setValidateAfterRun(boolean validateAfterRun) { this.validateAfterRun = validateAfterRun; }
    }

    /** 单城市长时数据库测试参数。 */
    public static class LongTest {
        private boolean enabled;
        private String feedId = "mybas-johor";
        private int durationMinutes = 120;
        private int cycles = 60;
        private int intervalSeconds = 120;
        private boolean validateAfterRun = true;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getFeedId() { return feedId; }
        public void setFeedId(String feedId) { this.feedId = feedId; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public int getCycles() { return cycles; }
        public void setCycles(int cycles) { this.cycles = cycles; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isValidateAfterRun() { return validateAfterRun; }
        public void setValidateAfterRun(boolean validateAfterRun) { this.validateAfterRun = validateAfterRun; }
    }

    /** 多城市数据库验收的 Feed、周期和间隔参数。 */
    public static class MulticityDbTest {
        private boolean enabled;
        private int durationMinutes = 10;
        private int cyclesPerFeed = 5;
        private int intervalSeconds = 120;
        private boolean validateAfterRun = true;
        private java.util.List<String> feeds = java.util.List.of("mybas-johor", "mybas-kuching", "rapid-bus-kl", "rapid-bus-mrtfeeder", "mybas-melaka");
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        public int getCyclesPerFeed() { return cyclesPerFeed; }
        public void setCyclesPerFeed(int cyclesPerFeed) { this.cyclesPerFeed = cyclesPerFeed; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public boolean isValidateAfterRun() { return validateAfterRun; }
        public void setValidateAfterRun(boolean validateAfterRun) { this.validateAfterRun = validateAfterRun; }
        public java.util.List<String> getFeeds() { return feeds; }
        public void setFeeds(java.util.List<String> feeds) { this.feeds = java.util.List.copyOf(feeds); }
    }

    /** IDEA/服务模式无固定时长连续运行与失败重试参数；validateAfterWindow仅保留旧配置兼容。 */
    public static class Continuous {
        private boolean enabled;
        private boolean validateAfterWindow;
        private int retryDelaySeconds = 30;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isValidateAfterWindow() { return validateAfterWindow; }
        public void setValidateAfterWindow(boolean validateAfterWindow) { this.validateAfterWindow = validateAfterWindow; }
        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(int retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
    }
}

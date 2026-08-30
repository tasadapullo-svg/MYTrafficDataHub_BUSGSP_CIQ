# IDEA Pre-Longrun Final Report

生成时间：2026-08-30（Asia/Shanghai）

## 1. Project Structure

唯一 Maven Root 为根 `pom.xml`；模块固定为 `Application / Common / BusGPS / CIQ`，Main 为 `com.mytransitgps.MyTrafficDataHubApplication`。

## 2. Java Version

已安装并实际使用 Eclipse Temurin `17.0.20.1`。Maven 3.9.9 的 runtime 为 JDK 17；根 POM `java.version=17`。

## 3. Application Test Fix

`CiqArchiveConditionContextTest` 使用命令行优先级强制 `test` profile，避免默认 `dev` 覆盖数据库关闭配置；仅在该无 DB 上下文排除真实 DataSource/MyBatis/MyBatis-Plus 自动配置，并使用临时工作区。

## 4. CIQ JDBC Conditional Beans

`CiqTrafficSpeedPersistenceService` 仅在 CIQ=true、database-write=true、JdbcTemplate 存在时创建；`CiqMonitoringRepository` 仅在 JdbcTemplate 存在时创建。新增 3 个条件装配测试全部 PASS。

## 5. BusGPS Regression

业务实现冻结；49 tests PASS。为避免 `clean` 后依赖旧 target/外部工程，将既有离线证据复制为根目录 `test-data/busgps`，未发送网络请求。

## 6. CIQ Tests

20 tests PASS；包括 batch-size=500 单次空间批查询、artifact 顺序、run finalization、study_area ready、Scheduler 防重入和 JDBC 条件 Bean。

## 7. IDEA Cleanup

旧 `.idea` 已可恢复地移至项目同级目录 `MYTrafficDataHub_20260829_pre_final_backup_20260830_1450/.idea`。重导入 SOP 已更新。

## 8. Legacy Path Cleanup

源码与文档扫描未发现旧嵌套工程名、旧 Main class、非 JDK17 字样或旧用户绝对路径。日志路径统一由 `TRAFFIC_FILE_LOG_ROOT` / `TrafficLogPathResolver` 解析。

## 9. Chinese Javadoc

项目自有 main Java 类型（含嵌套 class/interface/enum/record）类级中文 Javadoc 缺失数：0。

## 10. Chinese Logging

Application、WorkspaceRootResolver、MyBatisDatabaseConfig、CIQ API01 重点日志主体已中文化；技术词和结构化字段保留。

## 11. Study Area Source

Singapore Government OneMap Search API，检索时间与完整返回匹配记录见 `docs/ciq/STUDY_AREA_SOURCE_EVIDENCE.md`。

## 12. Woodlands Anchor

官方地址 `21 Woodlands Crossing, Singapore 738203`；选取唯一总关口记录 `WOODLANDS CHECKPOINT`：`POINT(103.7685958692307 1.445646787399312)`。门牌和邮编 PASS。

## 13. Tuas Anchor

官方地址 `501 Jalan Ahmad Ibrahim, Singapore 639937`；唯一结果 `TUAS CHECKPOINT COMPLEX`：`POINT(103.6354480925524 1.347293201169603)`。门牌和邮编 PASS。

## 14. Study Area Seed

已生成 `sql/lta/api01/01_seed_study_area.sql`：事务、advisory lock、geography buffer、ST_Multi、重复/既有 geometry 拒绝覆盖、六组合幂等处理。

## 15. Study Area Live DB Backup

已连接 `jdbc:postgresql://localhost:5432/postgres`（用户 `postgres`），确认 PostgreSQL 16.4 / PostGIS 3.4.3。写入前 `lta.study_area` 为 0 行；已生成空表基线备份 `sql/lta/api01/backups/study_area_backup_20260830_145910.csv`（含完整列头）。报告与源码未记录密码。

## 16. Geometry QA

六个 live geometry 均为非空、valid、SRID 4326、MULTIPOLYGON；面积分别为 Woodlands 3.122492 / 28.102429 / 78.062295 km²，Tuas 3.122161 / 28.099450 / 78.054021 km²。`STUDY_AREA_LIVE_GEOMETRY=PASS`。

## 17. Six Area QA

每个正式组合 active_count 严格等于 1；面积递增、APPROACH 覆盖 CORE、CORRIDOR 覆盖 APPROACH均为 true。两个官方锚点各命中自身 3 区且不命中另一 CIQ CORE。seed 复跑后 UID 与行数不变，幂等性 PASS。完整 UID 见 `docs/ciq/STUDY_AREA_LIVE_QA_REPORT.md`。

## 18. Spatial Link QA

`lta.traffic_link` 当前为 0 行，六区 `ST_Intersects` 统计均为 0；状态 `LINK_SCOPE_QA_PENDING_API01`。这不阻塞 study-area 就绪，需在用户后续 API01 `MANUAL_TEST` 后验证业务链与 Link 数量单调关系。

## 19. API01 Batch

保留 batch-size=500、VALUES CTE、批量空间查询；没有退回逐 Link SQL。

## 20. Artifact Chain

HTTP → Raw → SHA → collection_artifact → Parser 顺序测试 PASS。

## 21. Run Finalization

Raw/Parser/Spatial/DB 异常路径的 collection_run 闭合测试 PASS。

## 22. HttpClient

保留 JdkTrafficHttpClient 复用、Redirect.NEVER、host validation、timeout 与 retry。

## 23. Maven Compile

JDK 17：`.\mvnw.cmd clean compile`，PASS。

## 24. Maven Test

JDK 17：Common 2、BusGPS 49、CIQ 20、Application 8；0 failure、0 error、0 skipped，PASS。新增 `LongrunConfigurationTest` 验证 IDEA longrun 环境变量组合会打开 BusGPS 连续运行、CIQ API01 Scheduler、CIQ 数据库写入、每日归档，并保持 API01 batch size=500、manual-test=false、Redis 启动 Ping=false。

## 25. Maven Package

JDK 17：`.\mvnw.cmd clean package`，PASS。可执行 JAR：`Application/target/Application-0.0.1-SNAPSHOT.jar`；Start-Class 正确。

## 26. Remaining Blockers

代码启动和 CIQ 数据库写入前置条件均已满足。尚未执行的工作是用户侧真实运行验证：先开启 `CIQ_DATABASE_WRITE_ENABLED=true` 并执行一次 API01 `MANUAL_TEST`，再按 30 分钟 → 2–4 小时顺序运行。`traffic_link` 为空时的 Link Scope QA 仍为 PENDING，等待 MANUAL_TEST 产生业务数据。

## 27. User 30-Minute SOP

先 Raw-only MANUAL_TEST；仅在两个 Ready 都为 true 后执行 Manual DB Test。Manual E2E/Dedup/空间 QA 通过后，再由用户本人运行 30 分钟并检查 5 Feed、120 秒、DB/Latest/Dashboard、CIQ Raw/DB/Spatial、日志和归档。

## 28. User 2–4 Hour SOP

30 分钟 PASS 后由用户本人运行 2–4 小时；检查 API01/空间/DB batch 时长、skip、429/5xx、DB 错误、内存、磁盘、日志与归档。API01 经常超过 10 分钟则 PERFORMANCE_GATE=FAIL。

## Final Status

```ini
JAVA_VERSION=17
PROJECT_STRUCTURE=PASS
COMMON_TESTS=PASS
BUSGPS_TESTS=PASS
CIQ_TESTS=PASS
APPLICATION_TESTS=PASS
CIQ_ARCHIVE_CONTEXT=PASS
MYBATIS_DB_ENABLED_CONTEXT=PASS
STUDY_AREA_SOURCE=PASS
WOODLANDS_ANCHOR=PASS
TUAS_ANCHOR=PASS
WOODLANDS_CORE=PASS
WOODLANDS_APPROACH_3KM=PASS
WOODLANDS_CORRIDOR_5KM=PASS
TUAS_CORE=PASS
TUAS_APPROACH_3KM=PASS
TUAS_CORRIDOR_5KM=PASS
STUDY_AREA_LIVE_GEOMETRY=PASS
STUDY_AREA_QA=PASS
CIQ_DATABASE_WRITE_DEFAULT=false
MAVEN_COMPILE=PASS
MAVEN_TEST=PASS
MAVEN_PACKAGE=PASS
API02_API08_STATUS=NOT_STARTED_AS_REQUIRED
READY_FOR_IDEA_START=true
CIQ_DATABASE_WRITE_READY=true
CIQ_DATABASE_WRITE_ENABLED=false
CODE_READY_FOR_IDEA_TEST=true
REAL_LONGRUN_PASS=NOT_EXECUTED
```

两个 Ready 已分别满足：工程构建/测试可进入 IDEA，且 Woodlands/Tuas 六区已真实进入 PostGIS 并通过 QA。源码默认数据库写开关仍保持 false；由用户在 MANUAL_TEST 前显式开启。

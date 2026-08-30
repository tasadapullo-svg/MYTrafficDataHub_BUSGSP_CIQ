# IDEA Longrun Run Configuration

本文件只说明用户在 IDEA 中如何启动长期运行。Codex 不启动 Spring Boot、不执行真实 LTA HTTP、不执行 30 分钟或 2-4 小时 longrun。

## 启动前硬条件

只有以下两项都成立，才允许把 `CIQ_DATABASE_WRITE_ENABLED=true`：

```ini
READY_FOR_IDEA_START=true
CIQ_DATABASE_WRITE_READY=true
```

只有一次 API01 `MANUAL_TEST` 和此前 30 分钟测试都已经通过，才允许把 `CIQ_SCHEDULE_ENABLED=true`。如果任一前置验证还没通过，保持：

```ini
CIQ_SCHEDULE_ENABLED=false
```

## IDEA Run Configuration

在 IDEA 打开：

`Run -> Edit Configurations -> Application`

填写：

```text
Name: MYTrafficDataHub-longrun
Module: Application
Main class: com.mytransitgps.MyTrafficDataHubApplication
JRE: Java 17
Working directory: C:\Users\DELL\Desktop\MYTrafficDataHub_20260829
Active profiles: longrun
```

如果界面没有 `Active profiles`，在环境变量中加入：

```ini
SPRING_PROFILES_ACTIVE=longrun
```

## 长期运行环境变量

只在 IDEA Run Configuration 中填写实际值，不把数据库密码、LTA AccountKey 或其他 Secret 写入 YAML、源码、文档或日志。

```ini
SPRING_PROFILES_ACTIVE=longrun
TRAFFIC_DB_URL=jdbc:postgresql://localhost:5432/postgres
TRAFFIC_DB_USERNAME=postgres
TRAFFIC_DB_PASSWORD=<your-db-password>
TRAFFIC_WORKSPACE_ROOT=data_download
TRAFFIC_FILE_LOG_ROOT=C:\Users\DELL\Desktop\MYTrafficDataHub_20260829\data_download\logs
CIQ_ENABLED=true
CIQ_TRAFFIC_SPEED_ENABLED=true
CIQ_DATABASE_WRITE_ENABLED=true
CIQ_SCHEDULE_ENABLED=true
CIQ_MANUAL_TEST_ENABLED=false
CIQ_DAILY_ARCHIVE_ENABLED=true
LTA_BASE_URL=https://datamall2.mytransport.sg/ltaodataservice
LTA_ACCOUNT_KEY=<your-real-lta-account-key>
TRAFFIC_REDIS_CONNECT_ON_STARTUP=false
TRAFFIC_REDIS_HOST=127.0.0.1
TRAFFIC_REDIS_PORT=6379
TRAFFIC_REDIS_DATABASE=0
TRAFFIC_REDIS_PASSWORD=
```

IDEA 单行分号格式：

```text
SPRING_PROFILES_ACTIVE=longrun;TRAFFIC_DB_URL=jdbc:postgresql://localhost:5432/postgres;TRAFFIC_DB_USERNAME=postgres;TRAFFIC_DB_PASSWORD=<your-db-password>;TRAFFIC_WORKSPACE_ROOT=data_download;TRAFFIC_FILE_LOG_ROOT=C:\Users\DELL\Desktop\MYTrafficDataHub_20260829\data_download\logs;CIQ_ENABLED=true;CIQ_TRAFFIC_SPEED_ENABLED=true;CIQ_DATABASE_WRITE_ENABLED=true;CIQ_SCHEDULE_ENABLED=true;CIQ_MANUAL_TEST_ENABLED=false;CIQ_DAILY_ARCHIVE_ENABLED=true;LTA_BASE_URL=https://datamall2.mytransport.sg/ltaodataservice;LTA_ACCOUNT_KEY=<your-real-lta-account-key>;TRAFFIC_REDIS_CONNECT_ON_STARTUP=false;TRAFFIC_REDIS_HOST=127.0.0.1;TRAFFIC_REDIS_PORT=6379;TRAFFIC_REDIS_DATABASE=0;TRAFFIC_REDIS_PASSWORD=
```

## 当前代码配置语义

- 不需要修改 YAML。
- `longrun` profile 下 BusGPS 五个 Feed 全部开启。
- BusGPS 每 120 秒连续采集。
- BusGPS 数据库写入开启。
- CIQ API01 Scheduler 只由 `CIQ_SCHEDULE_ENABLED` 控制。
- CIQ 数据库写入只由 `CIQ_DATABASE_WRITE_ENABLED` 控制，默认仍为 `false`。
- API01 batch size 为 `500`。
- CIQ 每日归档由 `CIQ_DAILY_ARCHIVE_ENABLED` 控制。
- CIQ API01 Cron 为 `0 10,20,30,50 * * * *`。
- CIQ 时区为 `Asia/Kuala_Lumpur`。

## 启动前数据库确认

`lta.study_area` 中以下六条必须仍为 active，且 PostGIS QA 必须保持 PASS：

```text
WOODLANDS / CORE
WOODLANDS / APPROACH
WOODLANDS / CORRIDOR
TUAS / CORE
TUAS / APPROACH
TUAS / CORRIDOR
```

## 运行期间观察项

- BusGPS 五个 Feed 是否每 120 秒持续采集。
- API01 是否按每小时 `:10 / :20 / :30 / :50` 触发。
- `traffic_link -> traffic_link_scope -> traffic_speed_observation` 是否持续写入。
- 是否出现 HTTP 401、403、429 或 5xx。
- 是否出现数据库异常、Scheduler skip 或重复 Observation。
- API01 单轮耗时是否明显小于 10 分钟。
- Raw JSON、日志和归档是否正常增长。
- 内存是否持续增长而不释放。

长期运行初期建议用 IDEA `Run`，减少断点对调度时序的影响。

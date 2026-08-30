# MYTrafficDataHub

MYTrafficDataHub 是一个面向马来西亚公交 GPS（GTFS-Realtime / GTFS Static）和新加坡 LTA CIQ 交通数据的多源采集、校验、持久化、归档与可视化平台。

当前仓库是可运行工程快照，包含源码、SQL、前端、测试数据、构建产物和部分运行数据。仓库名称中的 `BUSGSP` 为历史命名，代码模块使用正确名称 `BusGPS`。

## 核心能力

- 采集 5 个马来西亚公交 Feed 的 GTFS-Realtime 车辆位置与 GTFS Static 数据。
- 采集 8 个新加坡 LTA DataMall CIQ 接口。
- PostgreSQL 16 + PostGIS 空间存储，支持按城市 Schema 路由。
- 原始文件、解析结果、质检结果、数据库记录和归档文件形成可追溯证据链。
- 支持交通速度空间筛选、车辆位置 QC、跳点检测、静态线路质检和时间完整性检查。
- Spring Boot 后端与 Vue 3 可视化大屏。
- 支持定时采集、失败重试、单 JVM 重入保护、每日归档和长期运行。

## 工程结构

```text
MYTrafficDataHub
├─ Common/       公共 HTTP、采集上下文、工具类和共享数据库配置
├─ BusGPS/       GTFS-Realtime / GTFS Static 采集、解析、QC、持久化和归档
├─ CIQ/          LTA CIQ API01–API08 采集、解析、校验、持久化和归档
├─ Application/  Spring Boot 启动模块、REST API 和已构建前端静态资源
├─ frontend/     Vue 3 + TypeScript + Vite 大屏源码
├─ sql/          PostgreSQL / PostGIS 建表、索引、约束和验证脚本
├─ docs/         架构、部署、迁移、存储和验证文档
├─ test-data/    本地回归与集成测试数据
└─ data_download/运行数据、Raw 文件、日志和归档快照
```

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 4.0.3、Spring JDBC |
| 数据访问 | MyBatis-Plus 3.5.16、JdbcTemplate |
| 数据库 | PostgreSQL 16、PostGIS 3.4 |
| 缓存 | Redis（启动连通性检查可选） |
| 前端 | Vue 3.5、TypeScript 5.9、Vite 7、ECharts 6、Leaflet 1.9 |
| 测试 | JUnit 5、Spring Boot Test、H2、Mockito |
| 构建 | Maven Wrapper、npm |

## 数据源

### Bus GPS

| Feed | 城市 | Provider | 数据类型 |
|---|---|---|---|
| `mybas-johor` | Johor Bahru | BAS.MY | GTFS-Realtime + GTFS Static |
| `mybas-kuching` | Kuching | BAS.MY | GTFS-Realtime + GTFS Static |
| `rapid-bus-kl` | Kuala Lumpur | Prasarana | GTFS-Realtime + GTFS Static |
| `rapid-bus-mrtfeeder` | Kuala Lumpur | Prasarana | GTFS-Realtime + GTFS Static |
| `mybas-melaka` | Melaka | BAS.MY | GTFS-Realtime + GTFS Static |

### CIQ / LTA DataMall

| API | 数据集 | 默认调度 |
|---|---|---|
| API01 | Traffic Speed Bands | 每 15 分钟 |
| API02 | Estimated Travel Times | 每 15 分钟 |
| API03 | Traffic Incidents | 每 5 分钟 |
| API04 | VMS | 每 5 分钟 |
| API05 | Faulty Traffic Lights | 每小时 |
| API06 | Road Works | 每日 |
| API07 | Traffic Flow | 每月 |
| API08 | Road Openings | 每日 |

CIQ 各接口拥有独立 Client、Parser、Validator、Persistence、Collector 和 Scheduler；公共编排层只负责分页、Raw 落盘、审计和异常降级。

## 环境要求

- Windows 10/11（项目附带 BAT 启动脚本）
- JDK 17
- PostgreSQL 16 + PostGIS
- Maven Wrapper 所需网络访问，或本地 Maven 3.9+
- Node.js 20+ 与 npm（仅前端开发需要）
- Redis 可选；默认不执行启动 Ping

## 配置

公共配置位于：

- `Application/src/main/resources/application.yml`
- `Application/src/main/resources/application-longrun.yml`
- `BusGPS/src/main/resources/config/bus-gps.yml`
- `CIQ/src/main/resources/config/ciq.yml`

推荐使用环境变量覆盖敏感配置：

```text
TRAFFIC_DB_URL
TRAFFIC_DB_USERNAME
TRAFFIC_DB_PASSWORD
TRAFFIC_REDIS_HOST
TRAFFIC_REDIS_PORT
TRAFFIC_REDIS_PASSWORD
TRAFFIC_WORKSPACE_ROOT
TRAFFIC_FILE_LOG_ROOT
LTA_ACCOUNT_KEY
CIQ_ENABLED
CIQ_DATABASE_WRITE_ENABLED
CIQ_SCHEDULE_ENABLED
```

生产或共享环境不应在 Git 历史中保存真实密码和 API Key。修改 YAML 或环境变量后必须重启应用。

## 数据库初始化

LTA/CIQ SQL 位于 `sql/lta/`，建议按编号执行：

```text
01_lta_precheck.sql
02_lta_schema.sql
03_lta_tables.sql
04_lta_comments.sql
05_lta_indexes_constraints.sql
06_lta_init_api_endpoint.sql
07_lta_verify.sql
```

Bus GPS 多城市 Schema 脚本位于 `sql/busgps/`。执行任何 DDL 前应先备份数据库，并在目标环境中核对 Schema、PostGIS 扩展和数据库用户权限。

## 构建与测试

```bat
mvnw.cmd clean test
mvnw.cmd clean package
```

默认 Maven 测试排除 `*IntegrationTest`。需要访问真实数据库或外部接口的测试，应在隔离环境中按对应测试说明单独执行。

生成的可执行 JAR：

```text
Application\target\Application-0.0.1-SNAPSHOT.jar
```

## 启动

项目根目录提供 Java 17 检查与 longrun Profile 启动脚本：

```bat
start_MYTrafficDataHub.bat
```

等价命令：

```bat
java -jar Application\target\Application-0.0.1-SNAPSHOT.jar --spring.profiles.active=longrun
```

默认端口为 `8080`。

## 前端开发

```bash
cd frontend
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

## 主要页面和接口

| 地址 | 说明 |
|---|---|
| `/api/health` | 服务健康检查 |
| `/dashboard` | 公交 GPS 大屏 |
| `/dashboard/ciq` | CIQ 大屏 |
| `/api/dashboard/*` | 公交 GPS 大屏数据接口 |
| `/api/ciq/dashboard/*` | CIQ 大屏数据接口 |

## 数据与日志

运行根目录由 `TRAFFIC_WORKSPACE_ROOT` 控制。默认使用 `data_download/`：

```text
data_download/
├─ BusGPS/
├─ CIQ/
├─ raw_data/
├─ run_reports/
├─ logs/
└─ runtime/
```

长期运行时不要直接删除 Raw、归档或数据库历史科研数据。先使用审计 SQL 和报告确认影响范围。

## 质量审计

- [CIQ API01/API02/API03 重复数据专项检查](CIQ_API01_02_03_DUPLICATE_CHECK_20260831.md)
- [CIQ 存储、归档与范围说明](docs/ciq/CIQ_STORAGE_ARCHIVE_AND_SCOPE.md)
- [IDEA 长期运行配置](docs/operation/IDEA_LONG_RUNNING_SETUP.md)
- [多模块迁移计划](docs/operation/MULTI_MODULE_MIGRATION_PLAN.md)

## 仓库说明

该仓库当前包含完整工程快照，包括构建产物、依赖目录和运行数据，因此体积明显大于常规源码仓库。后续若要用于团队协作，建议另建只包含源码与可复现测试夹具的精简分支，并使用 Git LFS 或对象存储管理大型归档。

## License

当前仓库未提供独立 `LICENSE` 文件。对外分发或开源前，请由项目所有者明确许可证和第三方数据使用条款。

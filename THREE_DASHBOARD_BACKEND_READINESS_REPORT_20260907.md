# THREE_DASHBOARD_BACKEND_READINESS_REPORT_20260907

## 1. 总体状态

**NOT READY（仅因当前执行容器无法完成正式 Maven/Vite 构建验证；变更源码级检查已通过，可交给本机 IDEA 继续最终联调）**

## 2. CIQ大屏

**PASS**

## 3. CIQ接口总览真实数据库数据

**PASS（代码/SQL映射检查）**

- API01–API08 均读取 PostgreSQL 业务表与 `lta.collection_run`。
- `latestDataTime`、`todayCount` 为真实业务表聚合。
- `totalCount` 使用 PostgreSQL `pg_stat_user_tables.n_live_tup` 的数据库行统计值，避免 API01/API02 大表首页全表 `COUNT(*)`。
- 新增 `lastSuccessTime`，同时保留原 `lastSuccess` 兼容字段。

## 4. CIQ API01–API08全部显示能力

**PASS**

## 5. CIQ“查询超时”前端显示

**REMOVED**

前端状态统一为：`数据加载中...` / `暂无数据` / `数据暂不可用`。

## 6. BusGPS大屏

**PASS（保持原设计和现有功能，未重写采集逻辑）**

## 7. BusGPS实时接口

**PASS（现有 `/api/dashboard/*` Controller → Service → Repository 真实数据库链路保持不变）**

## 8. CIQBus大屏

**PASS（源码与前端静态检查）**

## 9. CIQBus顶部切换

**PASS**

## 10. CIQBus实时地图

**PASS**

## 11. 160

**PASS**

## 12. 170

**PASS**

## 13. 170X

**PASS**

## 14. 950

**PASS**

## 15. SG_TO_JB统计

**PASS**

## 16. JB_TO_SG统计

**PASS**

## 17. CIQBus历史passage查询

**PASS（分页只读 SQL）**

## 18. CIQBus crossing查询

**PASS（分页只读 SQL）**

## 19. CIQBus hourly统计

**PASS**

支持 00:00–23:00、SG_TO_JB、JB_TO_SG、TOTAL、平均通过时间、Median、P90、P95。

## 20. Redis active读取

**PASS（代码逻辑检查）**

Dashboard 通过 `CiqBusEventStateStore.findActive()` 读取，不在 Controller 拼接 Redis Key，不执行 Redis 写操作。

## 21. Redis经纬度解析

**PASS**

地图只绘制非空、合法范围且非 `0,0` 的真实 active-event 经纬度。

## 22. PostgreSQL CIQ查询

**PASS（SQL/DDL静态映射检查；未连接用户真实数据库）**

## 23. PostgreSQL BusGPS查询

**PASS（现有真实数据库查询链路静态检查；未连接用户真实数据库）**

## 24. PostgreSQL CIQBus查询

**PASS（现有 persistence 字段 + 需求字段静态映射；未连接用户真实数据库）**

## 25. 中文

**PASS**

## 26. English

**PASS**

新增 CIQBus 62 个页面级 i18n Key 均检查到中英文双份定义。

## 27. Maven clean compile

**FAIL（执行环境阻断，不是已定位的源码编译错误）**

- 容器无全局 `mvn`。
- 项目 `./mvnw clean compile` 尝试下载 Maven 3.9.9 时失败：`Could not resolve host: repo.maven.apache.org`。
- 补充校验：使用上传工程原 Spring Boot JAR 中的依赖，对本轮全部变更 Java 文件执行 `javac --release 17`，结果 **PASS**。

## 28. Maven test

TOTAL=N/A  
PASS=N/A  
FAIL=N/A  
ERROR=ENVIRONMENT_BLOCKED

`./mvnw test` 同样因无法解析 `repo.maven.apache.org` 未进入测试执行阶段，因此没有伪造测试数量。

## 29. 前端静态检查

**PASS**

- `vue-tsc --noEmit`：**PASS**。
- 三轮导航/API/字段/i18n/空值/禁用项静态审计：**PASS**。
- 完整 `vite build`：当前上传的 `node_modules` 只带 Windows Rollup 原生包，当前执行容器为 Linux；补装 Linux Rollup 又因网络/DNS受限失败。因此需在用户 Windows 本机执行一次 `npm run build` 生成最新 `Application/src/main/resources/static/dashboard` 静态文件。

## 30. 新增后端API

- `GET /api/ciqbus/realtime`
- `GET /api/ciqbus/overview?date=YYYY-MM-DD`
- `GET /api/ciqbus/hourly?date=YYYY-MM-DD`
- `GET /api/ciqbus/passages?date=&route=&direction=&page=&size=`
- `GET /api/ciqbus/crossings?date=&route=&direction=&page=&size=`
- `GET /api/ciqbus/status`

现有并继续复用：

- CIQ：`/api/ciq/dashboard/*`
- BusGPS：`/api/dashboard/*`

## 31. 修改文件

- `Application/src/main/java/com/mytransitgps/dashboard/controller/DashboardPageController.java`
- `CIQ/src/main/java/com/mytransitgps/modules/ciq/dashboard/dto/CiqDashboardDtos.java`
- `CIQ/src/main/java/com/mytransitgps/modules/ciq/dashboard/repository/CiqDashboardRepository.java`
- `CIQ/src/main/java/com/mytransitgps/modules/ciq/dashboard/service/CiqDashboardService.java`
- `frontend/src/api/ciqDashboardApi.ts`
- `frontend/src/components/dashboard/DashboardHeader.vue`
- `frontend/src/i18n/index.ts`
- `frontend/src/main.ts`
- `frontend/src/router.ts`
- `frontend/src/views/CiqDashboardView.vue`

## 32. 新增文件

- `Application/src/main/java/com/mytransitgps/dashboard/controller/CiqBusDashboardController.java`
- `CIQBus/src/main/java/com/mytransitgps/modules/ciqbus/dashboard/dto/CiqBusDashboardDtos.java`
- `CIQBus/src/main/java/com/mytransitgps/modules/ciqbus/dashboard/repository/CiqBusDashboardRepository.java`
- `CIQBus/src/main/java/com/mytransitgps/modules/ciqbus/dashboard/service/CiqBusDashboardService.java`
- `frontend/src/api/ciqBusDashboardApi.ts`
- `frontend/src/components/ciq/CiqBusHourlyChart.vue`
- `frontend/src/components/ciq/CiqBusMap.vue`
- `frontend/src/styles/ciqbus-dashboard.css`
- `frontend/src/views/CiqBusDashboardView.vue`
- `THREE_DASHBOARD_BACKEND_READINESS_REPORT_20260907.md`

## 33. 删除文件

**无**

## 34. 当前遗留问题

1. 当前执行容器无法联网下载 Maven 3.9.9，因此正式 `mvn clean compile` / `mvn test` 必须由用户本机 IDEA/Maven 完成。
2. 当前上传包的 `frontend/node_modules` 是 Windows 原生依赖，而执行容器是 Linux；用户本机替换文件后需在 `frontend` 执行 `npm run build`，再启动 IDEA，确保 Spring Boot 使用最新前端静态资源。
3. 按本轮边界未连接、修改或清理用户真实 PostgreSQL/Redis；真实数据结果、Redis active key 内容和运行时 SQL 性能需在用户本机最终联调确认。
4. CIQBus `ciq_bus_stop_passage.create_time` 在本轮需求中明确要求展示，但该表的 CREATE TABLE DDL 未包含在上传工程中；新查询按需求读取该字段。若用户本机真实表字段名称不同，需要按实际 DDL 做一处字段映射调整。

## 35. 是否已经准备好交给用户本机IDEA测试

**YES（源代码已准备好；先在 Windows 本机执行前端 `npm run build`，随后用 IDEA 执行 Maven compile/test 和完整启动联调）**

代码已准备好，可使用本机 IDEA 进行完整启动测试；本轮未进行长时间采集、生产环境修改、Redis 清理、数据库破坏性操作或 Git push。

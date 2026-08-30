# CIQ API01 / API02 / API03 数据库重复数据专项检查报告

## 1. 审计结论

审计时间：2026-08-31 01:46:50（数据库会话时间）  
数据库：PostgreSQL 16.4，数据库 `postgres`，只读事务  
范围：CIQ API01、API02、API03；代码、Raw JSON、PostgreSQL 表、约束、索引、Scheduler 与分页链路  
变更：未修改 Java、SQL、Mapper、数据库结构或历史数据；仅新增本报告并更新 README

| API | 主业务表 | 总行数 | 真正业务重复组 | 真正重复行 | 结论 |
|---|---|---:|---:|---:|---|
| API01 | `lta.traffic_speed_observation` | 52,934 | 0 | 0 | PASS，但存在源时间未入库的防护缺口 |
| API02 | `lta.travel_time_observation` | 4,224 | 0 | 0 | PASS |
| API03 | `lta.traffic_incident_event` | 73 | 0 | 0 | PASS |

三张业务表的 UUID 重复组均为 0。未执行任何 `DELETE`、`UPDATE`、`INSERT`、DDL、清表或重建操作。

本轮发现一类非业务表重复：API01 的 `lta.collection_artifact` 中，同一 `run_uid + file_path + sha256` 被重复登记，存在 853 个重复组、853 个额外行。该问题不等同于交通速度观测重复，根因是 Collector 同一页面调用了两次 `registerArtifact`。

## 2. 审计方法与证据边界

1. 从真实代码追踪 Scheduler → Collector → Client → Parser → Validator → Persistence → PostgreSQL。
2. 在 PostgreSQL 中开启 `SET TRANSACTION READ ONLY`，查询真实表结构、约束、索引和数据。
3. 检查主键 UUID、当前唯一键和建议业务键的重复组。
4. 读取最近 Raw JSON，并检查顶层与记录级时间字段。
5. 对 API01 解压日归档，将 1,113 个 Raw 页面、555,861 条原始记录与数据库 52,934 条研究区观测按 `run_uid / LinkID` 全量对齐。
6. 检查 Scheduler 重入保护、重复计划时间、分页 URI、Raw SHA 和 artifact 登记。

数据库在正常采集，报告数字是上述审计时点的快照，后续总行数会继续增长。

## 3. API 与真实表映射

```text
API01 TrafficSpeedBands
  -> lta.traffic_link
  -> lta.traffic_speed_observation（主观测表）
  -> lta.traffic_link_scope

API02 EstimatedTravelTimes
  -> lta.travel_time_segment
  -> lta.travel_time_observation（主观测表）

API03 TrafficIncidents
  -> lta.traffic_incident_event（生命周期主表）
```

所有 API 还会使用 `lta.api_endpoint`、`lta.collection_run`、`lta.collection_page_log` 和 `lta.collection_artifact` 保存采集审计信息。

## 4. API01：TrafficSpeedBands

### 4.1 调用链

| 节点 | 实际实现 |
|---|---|
| Scheduler | `CiqCollectionScheduler` |
| Collector | `CiqTrafficSpeedCollector` |
| Client | `LtaTrafficSpeedClient` |
| DTO / Domain | `TrafficSpeedBandRecord` |
| Parser | `TrafficSpeedBandsParser` |
| Validator | `CiqTrafficSpeedValidator` |
| Persistence | `CiqTrafficSpeedPersistenceService` |
| Repository / Mapper | 无独立 Repository/Mapper；使用 `JdbcTemplate` 内联 SQL |
| DDL | `sql/lta/03_lta_tables.sql` |
| Table | `lta.traffic_speed_observation` |

### 4.2 Raw JSON 与时间语义

API01 Raw 顶层字段：

```text
lastUpdatedTime
odata.metadata
value
```

记录字段：

```text
LinkID, RoadName, RoadCategory,
SpeedBand, MinimumSpeed, MaximumSpeed,
StartLon, StartLat, EndLon, EndLat
```

Raw 中存在明确的 `lastUpdatedTime`，但当前代码在 `CiqTrafficSpeedCollector` 第 79 行使用采集开始时间 `start` 作为固定 `snapshot_time`。DDL 已有 `source_updated_time` 字段，但 Persistence 的 INSERT 没有写入该字段。

一次 API01 分页采集可包含 2–3 个不同 `lastUpdatedTime`。这说明源时间应按页面解析并写入每条观测及 `collection_page_log.source_updated_time`，不能仅在整次 Run 上保存一个时间。

### 4.3 表字段

`lta.traffic_speed_observation`：

```text
uid UUID NOT NULL
snapshot_time TIMESTAMPTZ NOT NULL
link_uid UUID NOT NULL
speed_band SMALLINT NOT NULL
minimum_speed SMALLINT NULL
maximum_speed SMALLINT NULL
source_updated_time TIMESTAMPTZ NULL
run_uid UUID NOT NULL
create_time TIMESTAMPTZ NOT NULL
update_time TIMESTAMPTZ NOT NULL
```

### 4.4 约束与索引

- Primary Key：`uid`
- Unique：`(snapshot_time, link_uid)`
- Foreign Key：`link_uid -> lta.traffic_link(uid)`
- Foreign Key：`run_uid -> lta.collection_run(uid)`
- 普通索引：`(link_uid, snapshot_time DESC)`、`snapshot_time DESC`、`run_uid`
- BRIN：`snapshot_time`

当前存在数据库层去重保护，但保护的是“采集时间 + Link”，不是更准确的“源更新时间 + Link”。

### 4.5 重复审计结果

```text
Total rows                         = 52,934
UUID duplicate groups             = 0
UUID duplicate rows               = 0
snapshot_time + link_uid groups   = 0
snapshot_time + link_uid rows     = 0
source_updated_time populated     = 0 / 52,934
```

为了避免空 `source_updated_time` 导致假 PASS，本轮使用 Raw `lastUpdatedTime + LinkID` 重新审计：

```text
Database runs with observations           = 4
Matched Raw pages                          = 1,113
Raw records scanned                        = 555,861
Database-scope Raw records matched         = 52,934
lastUpdatedTime + LinkID duplicate groups  = 0
lastUpdatedTime + LinkID duplicate rows    = 0
same key + same speed content groups       = 0
same key + same speed content rows         = 0
```

分页专项结论：在已入库研究区 Link 的全量 Raw 中，没有发现页面重叠造成的 `lastUpdatedTime + LinkID` 重复；同一 Run 内也没有同一 Link 跨多个源时间重复出现。当前观测数据没有真正业务重复。

### 4.6 实际发现的问题

API01 Collector 在同一页面流程中两次调用：

```text
CiqTrafficSpeedCollector.java:100  registerArtifact(...)
CiqTrafficSpeedCollector.java:116  registerArtifact(...)
```

数据库证据：

```text
lta.collection_artifact duplicate groups = 853
extra duplicate artifact rows             = 853
```

Root cause：同一 Raw 页面在解析前后各登记一次，`collection_artifact` 又没有对应唯一约束。这是审计元数据重复，不是 `traffic_speed_observation` 业务重复。

### 4.7 API01 判定

```text
Table: lta.traffic_speed_observation
Total rows: 52,934
Business key: lastUpdatedTime/source_updated_time + link_uid
Duplicate groups: 0
Duplicate rows: 0
Root cause: 业务表无重复；artifact 重复由同页双重 registerArtifact 引起
Code modified: NO
Database modified: NO
Result: PASS（存在源时间未持久化的控制缺口）
```

## 5. API02：EstimatedTravelTimes

### 5.1 调用链

| 节点 | 实际实现 |
|---|---|
| Scheduler | `CiqApi02Scheduler` |
| Collector | `CiqEstimatedTravelTimesCollector` + `CiqPagedJsonCollectorSupport` |
| Client | `LtaEstimatedTravelTimesClient` |
| DTO / Domain | `EstimatedTravelTimeRecord` |
| Parser | `EstimatedTravelTimesParser` |
| Validator | `EstimatedTravelTimesValidator` |
| Persistence | `EstimatedTravelTimesPersistenceService` |
| Repository / Mapper | 无独立 Repository/Mapper；使用 `JdbcTemplate` 内联 SQL |
| Tables | `lta.travel_time_segment`、`lta.travel_time_observation` |

### 5.2 Raw JSON 与业务键

Raw 顶层没有源时间字段。记录字段为：

```text
Name, Direction, FarEndPoint, StartPoint, EndPoint, EstTime
```

自然业务实体键：

```text
name + direction + far_end_point + start_point + end_point
```

在没有源时间的情况下，一次 Collector Run 代表一次完整 Snapshot，因此更直接的观测去重键应为：

```text
run_uid + segment_uid
```

当前实现使用同一 Run 固定的 `snapshot_time=start`，数据库唯一键为 `(snapshot_time, segment_uid)`；在现有单 Snapshot/Run 流程下与上述语义等价，但数据库没有显式保护 `(run_uid, segment_uid)`。

### 5.3 主要字段、约束与索引

`lta.travel_time_segment`：

```text
uid, name, direction, far_end_point, start_point, end_point,
area_uid, active, first_seen_time, last_seen_time, create_time, update_time
```

唯一约束：

```text
(name, direction, far_end_point, start_point, end_point)
```

`lta.travel_time_observation`：

```text
uid, snapshot_time, segment_uid, est_time_min, run_uid, create_time, update_time
```

约束与索引：

- Primary Key：`uid`
- Unique：`(snapshot_time, segment_uid)`
- Foreign Key：`segment_uid -> lta.travel_time_segment(uid)`
- Foreign Key：`run_uid -> lta.collection_run(uid)`
- 索引：`(segment_uid, snapshot_time DESC)`、`snapshot_time DESC`、`run_uid`
- Persistence 使用 `ON CONFLICT(snapshot_time, segment_uid) DO NOTHING`

### 5.4 重复审计结果与判定

```text
Table: lta.travel_time_observation
Total rows: 4,224
Segment rows: 192
Runs: 22
Business key: run_uid + segment_uid
UUID duplicate groups: 0
Business duplicate groups: 0
Business duplicate rows: 0
Root cause: 未发现重复；现有 upsert/unique 保护有效
Code modified: NO
Database modified: NO
Result: PASS
```

## 6. API03：TrafficIncidents

### 6.1 调用链

| 节点 | 实际实现 |
|---|---|
| Scheduler | `CiqApi03Scheduler` |
| Collector | `CiqTrafficIncidentsCollector` + `CiqPagedJsonCollectorSupport` |
| Client | `LtaTrafficIncidentsClient` |
| DTO / Domain | `TrafficIncidentRecord` |
| Parser | `TrafficIncidentsParser` |
| Validator | `TrafficIncidentsValidator` |
| Persistence | `TrafficIncidentsPersistenceService` |
| Repository / Mapper | 无独立 Repository/Mapper；使用 `JdbcTemplate` 内联 SQL |
| Table | `lta.traffic_incident_event` |

### 6.2 Raw JSON 与业务键

Raw 顶层没有源时间字段。记录字段为：

```text
Type, Latitude, Longitude, Message
```

Persistence 将以下规范化内容计算 SHA-256：

```text
normalize(type) + latitude + longitude + normalize(message)
```

得到 `event_fingerprint`，作为事件生命周期业务键。相同事件在后续轮次不会新增行，而是更新 `last_seen_time / last_run_uid / active`。

### 6.3 表字段、约束与索引

```text
uid, event_fingerprint, type, latitude, longitude, geom,
message, area_uid, first_seen_time, last_seen_time, resolved_time,
active, first_run_uid, last_run_uid, create_time, update_time
```

- Primary Key：`uid`
- Unique：`event_fingerprint`
- Foreign Key：`area_uid`、`first_run_uid`、`last_run_uid`
- 索引：`active`、`type`、`first_seen_time`、`area_uid`、Run UID、GIST `geom`
- Persistence 使用 `ON CONFLICT(event_fingerprint) DO UPDATE`

### 6.4 重复审计结果与判定

```text
Table: lta.traffic_incident_event
Total rows: 73
Runs: 70
Business key: event_fingerprint
UUID duplicate groups: 0
Business duplicate groups: 0
Business duplicate rows: 0
Repeated Raw SHA groups across runs: 11
Repeated Raw extra run copies: 25
Root cause: 重复 Raw Snapshot 被生命周期 UPSERT 正确吸收，没有生成重复事件行
Code modified: NO
Database modified: NO
Result: PASS
```

重复 Raw SHA 是上游在多个轮次返回相同活动事件集合的正常现象。数据库 0 重复说明 API03 幂等逻辑有效。

## 7. Scheduler、分页与批次检查

### 7.1 Scheduler

- API01、API02、API03 都使用 `AtomicBoolean.compareAndSet(false, true)` 防止同一 JVM 内任务重叠。
- 数据库未发现相同 `api_code + scheduled_time` 的多 Run 记录。
- 当前保护不覆盖多 JVM/多主机部署。如果未来水平扩容，应增加 PostgreSQL advisory lock 或 ShedLock；当前单实例没有证据需要立即引入新框架。

### 7.2 Pagination

- API01 优先使用响应 `nextLink`，缺失时按 `$skip = pageNo * pageSize` 继续。
- 使用 `visited URI` 检测 URI 循环。
- API01 全量 Raw 审计没有发现 `lastUpdatedTime + LinkID` 页间重复。
- API02/03 使用固定 page size 和 `$skip`；当前数据量均为单页或正常结束。

### 7.3 批次与文件

- UUID 重复：0。
- 相同计划时间重复 Run：0。
- API01 artifact 重复：853 组，根因明确为双重登记。
- API03 相同 Raw SHA 跨 Run：存在，但业务表通过 fingerprint upsert 正确幂等。

## 8. 修改建议（本轮未执行）

| 优先级 | 范围 | 建议 | 原因 |
|---|---|---|---|
| P1 | API01 Collector/Persistence | 解析每页 `lastUpdatedTime`，写入 `collection_page_log.source_updated_time` 和 `traffic_speed_observation.source_updated_time` | 建立真实源 Snapshot 语义 |
| P1 | API01 Database | 在历史数据评估后增加非空部分唯一索引 `(source_updated_time, link_uid)` | 防止同一源 Snapshot 重复入库 |
| P1 | API01 Artifact | 删除一次重复 `registerArtifact` 调用，并为 `(run_uid, artifact_type, file_path)` 增加唯一保护 | 已有 853 组审计元数据重复 |
| P2 | API02 Database | 增加或改用 `(run_uid, segment_uid)` 显式唯一保护 | 与“无源时间时以 Run 表示 Snapshot”的业务语义一致 |
| P2 | API01/02 日志 | 每 Run 输出 received/validated/inserted/duplicateSkipped/failed 汇总 | 方便长期幂等监控 |
| P2 | API03 Persistence | 只在完整 Snapshot 全部分页处理完成后统一执行 resolved 判定 | 避免未来多页时前一页事件被后一页误判为消失 |
| P3 | Scheduler | 仅在多实例部署时增加数据库锁/ShedLock | `AtomicBoolean` 只保护单 JVM |

建议索引草案，仅供审核，未执行：

```sql
CREATE UNIQUE INDEX uq_traffic_speed_source_link
ON lta.traffic_speed_observation(source_updated_time, link_uid)
WHERE source_updated_time IS NOT NULL;

CREATE UNIQUE INDEX uq_travel_time_run_segment
ON lta.travel_time_observation(run_uid, segment_uid);

CREATE UNIQUE INDEX uq_collection_artifact_run_path
ON lta.collection_artifact(run_uid, artifact_type, file_path);
```

执行任何索引前必须先在当时数据库重新运行重复审计。API01 历史 `source_updated_time` 全为空，不能用 `snapshot_time` 盲目回填；需要从已归档 Raw JSON 按 Run/Page 恢复真实 `lastUpdatedTime`。

## 9. 对原审计 Prompt 的改进意见

原 Prompt 覆盖面较完整，但建议调整以下内容，提高可执行性和结论精度：

1. 将“只读审计”和“实施修复”拆成两个独立阶段。只读阶段不应同时要求修改代码和真实回归写入。
2. 增加统一审计时间点。系统持续采集时，总行数会变化，所有结果必须带数据库快照时间。
3. 将结果拆成三类：业务数据重复、审计元数据重复、去重控制缺口。三者不能共用一个 PASS/FAIL。
4. API01 必须强制报告 `source_updated_time` 非空覆盖率；覆盖率为 0 时，不能只凭 `(snapshot_time, link_uid)=0` 宣告完全安全。
5. 对生命周期表单独定义重复。API03 同一事件跨轮次更新一行是正确行为，不应按“每 Snapshot 一行”的标准判断。
6. 没有源时间的 API 应明确指定 `run_uid` 为 Snapshot 身份，而不是模糊选择 `create_time` 或 `request_time`。
7. 分页审计应检查每页源时间是否一致、页间自然键重叠、分页过程中源数据变化，而不仅检查相邻边界一条记录。
8. 增加只读事务要求：`SET TRANSACTION READ ONLY`，并明确禁止调用会触发采集或写库的 Spring 测试。
9. 在 PASS/FAIL 之外增加 `PASS_WITH_CONTROL_GAP` 和 `NOT_VERIFIABLE`，避免证据不足时被迫给出错误二元结论。
10. Build 与写入回归只在实际修改 Java/SQL 后执行；纯审计不应启动真实 Collector 或生成新数据库记录。

## 10. 最终结果

```text
1. API01
   Table = lta.traffic_speed_observation
   Total = 52,934
   Duplicate groups = 0
   Duplicate rows = 0
   Dedup key = source_updated_time/lastUpdatedTime + link_uid
   Root cause = 业务表无重复；artifact 双重登记导致 853 组审计元数据重复
   Modified = NO
   Result = PASS_WITH_CONTROL_GAP

2. API02
   Table = lta.travel_time_observation
   Total = 4,224
   Duplicate groups = 0
   Duplicate rows = 0
   Dedup key = run_uid + segment_uid
   Root cause = 未发现重复，现有 unique + ON CONFLICT 有效
   Modified = NO
   Result = PASS

3. API03
   Table = lta.traffic_incident_event
   Total = 73
   Duplicate groups = 0
   Duplicate rows = 0
   Dedup key = event_fingerprint
   Root cause = 重复 Raw Snapshot 被 lifecycle upsert 正确吸收
   Modified = NO
   Result = PASS

4. 修改文件
   README.md
   CIQ_API01_02_03_DUPLICATE_CHECK_20260831.md

5. Database constraint/index
   未修改；三张业务表均已有数据库唯一保护

6. Persistence
   未修改

7. Build
   NOT REQUIRED（未修改 Java/SQL）

8. 新数据正常入库
   PASS（审计期间持续产生新 Run 与新 Snapshot）

9. 新重复数据拦截
   API02/API03 = PASS
   API01 = 当前 snapshot_time 键 PASS，真实 source_updated_time 键存在控制缺口

10. 历史重复数据删除
    NO
```

最终判定：

```text
CIQ API01 BUSINESS DUPLICATE = PASS
CIQ API01 DEDUP CONTROL      = PASS_WITH_CONTROL_GAP
CIQ API02 DEDUP              = PASS
CIQ API03 DEDUP              = PASS
```

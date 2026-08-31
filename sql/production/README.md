# MYTrafficDataHub 生产空库初始化

本目录用于创建 Bus GPS 与 CIQ/LTA 的完整生产数据库结构，不迁移任何现有表数据。

## 文件

1. `00_create_database.sql`：创建 UTF-8 数据库 `mytrafficdatahub`。
2. `01_gps_ciq_schema_only.sql`：创建扩展、6 个业务 Schema、69 张表、约束、索引、函数、触发器和注释。
3. `02_verify_empty_schema.sql`：验证表数量和所有表均为 0 行。
4. `03_required_reference_seed.sql`：初始化应用运行必需的 4 个城市、5 个 GTFS Feed 和 8 个 CIQ API 配置，不包含历史业务数据。

## 执行顺序

### 第一步：创建数据库

在 DBeaver 中连接 PostgreSQL 自带的维护库 `postgres`，打开 Auto-commit，单独执行：

```sql
CREATE DATABASE mytrafficdatahub
    WITH TEMPLATE = template0
    ENCODING = 'UTF8';
```

如果生产数据库已经存在，跳过此步骤，不要重复执行。

### 第二步：连接新数据库

在 DBeaver 中新建或切换连接到：

```text
database = mytrafficdatahub
```

以具有 `CREATE` 权限并能够安装扩展的数据库账号，完整执行 `01_gps_ciq_schema_only.sql`。

服务器必须已经安装 PostGIS；脚本会执行：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 第三步：验证

在同一个 `mytrafficdatahub` 数据库中执行 `02_verify_empty_schema.sql`。成功时应看到：

```text
PASS: 69 GPS/CIQ tables created and all tables contain zero rows.
```

### 第四步：初始化必要参考配置

空库验证通过后执行 `03_required_reference_seed.sql`。预期结果：

```text
core.study_city = 4
core.gtfs_feed   = 5
lta.api_endpoint = 8
```

预期表数量：

| Schema | 用途 | 表数 |
|---|---|---:|
| `core` | GPS 公共采集、Feed 与归档元数据 | 4 |
| `jb` | Johor Bahru GPS | 12 |
| `kuching` | Kuching GPS | 12 |
| `kl` | Kuala Lumpur GPS（含 Rapid Bus 与 MRT Feeder） | 12 |
| `melaka` | Melaka GPS | 12 |
| `lta` | CIQ API01–API08 | 17 |
| 合计 |  | 69 |

## 应用配置

生产应用的数据源 URL 应指向新数据库，例如：

```text
TRAFFIC_DB_URL=jdbc:postgresql://<生产数据库IP>:5432/mytrafficdatahub
```

用户名和密码通过 `TRAFFIC_DB_USERNAME`、`TRAFFIC_DB_PASSWORD` 配置，不要写入 Git。

## 重要说明

- 本套脚本严格不包含 `INSERT` 或 `COPY`，执行后所有业务表均为 0 行。
- `core.study_city`、`core.gtfs_feed` 和 `lta.api_endpoint` 也是空表。它们属于应用运行所需的参考配置；在录入生产配置前不要启用 GPS/CIQ Scheduler。
- 工程原有 `sql/lta/06_lta_init_api_endpoint.sql` 会插入 CIQ 接口配置，本次按“不要表数据”的要求没有合并。
- `jbsp` 是旧结构，不属于当前五个 GPS Feed 的数据库路由，因此没有纳入生产业务脚本。
- 不要在已有业务数据的数据库中把本脚本当作迁移脚本使用；它面向新建空库。
- 建库与安装扩展通常需要数据库管理员权限，日常应用账号可在初始化后降权。

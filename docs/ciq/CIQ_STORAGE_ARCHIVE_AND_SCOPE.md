# CIQ JSON、归档与空间数据库安全门

CIQ Raw JSON 保存在 `<TRAFFIC_WORKSPACE_ROOT>/CIQ/yyyyMMdd/API01...API08/`。模块启用时只预建八个接口目录；API02–API08 不创建 Collector。

每日 00:30（`Asia/Kuala_Lumpur`）归档前一日目录。ZIP 内逐文件 SHA-256 校验成功后才删除源目录；失败时保留源数据。

## API01 数据链

```text
LTA 完整 response → Raw 100% 保存 → SHA/artifact → Parser/QC
→ ST_Intersects(study_area, candidate link) → PostgreSQL 范围内业务数据
```

`lta.study_area` 是唯一研究范围 Source of Truth，包含 Woodlands/Tuas 的 CORE 1 km、APPROACH 3 km、CORRIDOR 5 km 六个 active Polygon。空间筛选保留相交的双向道路，不按 RoadCategory 或 midpoint 预删道路。

数据库写入默认关闭：

```yaml
traffic:
  ciq:
    persistence:
      database-write-enabled: false
```

只有 live backup、seed 和 PostGIS QA 全部通过、且 `CIQ_DATABASE_WRITE_READY=true` 后，用户才可在 IDEA 中临时设置 `CIQ_DATABASE_WRITE_ENABLED=true`。Raw JSON 保存不受 study_area 影响。

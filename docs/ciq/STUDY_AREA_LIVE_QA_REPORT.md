# Study Area Live QA Report

执行时间：2026-08-30 14:59（Asia/Shanghai）  
数据库：`jdbc:postgresql://localhost:5432/postgres`（用户 `postgres`）  
数据库版本：PostgreSQL 16.4 / PostGIS 3.4.3  
状态：`PASS`

## 执行范围与安全边界

- 写入前只读检查确认 `lta.study_area` 和 `lta.traffic_link` 均为 0 行。
- 写入前备份：`sql/lta/api01/backups/study_area_backup_20260830_145910.csv`（0 条数据，保留完整列头作为空表基线）。
- 只执行 `lta.study_area` 六区 seed；未启动 Spring Boot、未调用 LTA HTTP、未写入 `traffic_link`、`traffic_link_scope` 或 `traffic_speed_observation`。
- seed 第二次执行成功且 UID、行数不变，幂等性 PASS。

## 锚点来源

- WOODLANDS：官方地址 `21 Woodlands Crossing, Singapore 738203`，OneMap 返回 `WOODLANDS CHECKPOINT`，`POINT(103.7685958692307 1.445646787399312)`。
- TUAS：官方地址 `501 Jalan Ahmad Ibrahim, Singapore 639937`，OneMap 返回 `TUAS CHECKPOINT COMPLEX`，`POINT(103.6354480925524 1.347293201169603)`。
- 完整来源与检索时间见 `docs/ciq/STUDY_AREA_SOURCE_EVIDENCE.md`。

## 六区 Live QA

| CIQ | Zone | Radius | Database UID | Geometry | Area km² | Row QA |
|---|---|---:|---|---|---:|---|
| WOODLANDS | CORE | 1 km | `9ce9a49e-1bca-479f-9410-91d6cbd8ee55` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 3.122492 | PASS |
| WOODLANDS | APPROACH | 3 km | `e628771e-e0d7-4e38-a25b-fa644e883293` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 28.102429 | PASS |
| WOODLANDS | CORRIDOR | 5 km | `68e99079-a6cb-4e52-8f1a-4ca9b6facb74` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 78.062295 | PASS |
| TUAS | CORE | 1 km | `553b2d79-bd8b-42a5-be3a-ba39d0cf52fe` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 3.122161 | PASS |
| TUAS | APPROACH | 3 km | `7515841a-5dd7-4102-8666-7e1f0ee4037e` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 28.099450 | PASS |
| TUAS | CORRIDOR | 5 km | `2b0db8e7-bf6f-42d2-9d38-86ad1101fea3` | MULTIPOLYGON / SRID 4326 / valid / non-empty | 78.054021 | PASS |

每个目标组合的 active count 均严格等于 1，area name 与正式命名一致。

## 空间关系 QA

| CIQ | CORE < APPROACH < CORRIDOR | APPROACH covers CORE | CORRIDOR covers APPROACH | 本锚点命中区数 | 误命中另一 CIQ CORE | Status |
|---|---|---|---|---:|---:|---|
| WOODLANDS | true | true | true | 3 | 0 | PASS |
| TUAS | true | true | true | 3 | 0 | PASS |

## Road Link QA

`lta.traffic_link` 当前为 0 行，六区交集数量均为 0，因此状态为：

`LINK_SCOPE_QA_PENDING_API01`

这不是 study-area QA 失败。待用户开启数据库写入并完成一次 API01 `MANUAL_TEST` 后，再核验 `traffic_link → traffic_link_scope → traffic_speed_observation` 以及六区 Link 数量的单调关系。

## Final Status

```ini
STUDY_AREA_LIVE_GEOMETRY=PASS
STUDY_AREA_QA=PASS
CIQ_DATABASE_WRITE_READY=true
CIQ_DATABASE_WRITE_ENABLED=false
```

# CIQ Study Area 锚点来源证据

检索时间：2026-08-30T14:35:40.281454+08:00（Asia/Shanghai）  
地理编码来源：Singapore Government OneMap Search API（`https://www.onemap.gov.sg/api/common/elastic/search`）  
坐标系：WGS84，经度/纬度（SRID 4326）

## WOODLANDS

- 官方地址：21 Woodlands Crossing, Singapore 738203
- 查询：`21 Woodlands Crossing Singapore 738203`
- 选定结果：`SEARCHVAL=WOODLANDS CHECKPOINT`
- 返回地址：`21 WOODLANDS CROSSING WOODLANDS CHECKPOINT SINGAPORE 738203`
- 门牌/邮编核对：`21` / `738203`，PASS
- 经度：`103.7685958692307`
- 纬度：`1.445646787399312`
- 判定说明：同一查询返回 6 个同地址建筑结果；其中只有一个总记录的 `SEARCHVAL` 与 `BUILDING` 均为 `WOODLANDS CHECKPOINT`，其余为 BLK A–E，因此没有按相似度猜选。

## TUAS

- 官方地址：501 Jalan Ahmad Ibrahim, Singapore 639937
- 查询：`501 Jalan Ahmad Ibrahim Singapore 639937`
- 选定结果：`SEARCHVAL=TUAS CHECKPOINT COMPLEX`
- 返回地址：`501 JALAN AHMAD IBRAHIM TUAS CHECKPOINT COMPLEX SINGAPORE 639937`
- 门牌/邮编核对：`501` / `639937`，PASS
- 经度：`103.6354480925524`
- 纬度：`1.347293201169603`
- 判定说明：查询仅返回 1 个结果，地址、门牌和邮编全部精确匹配。

## Polygon 生成规则

没有使用猜测坐标或 Java WKT 常量。`sql/lta/api01/01_seed_study_area.sql` 以以上官方地址锚点创建 `1,000 / 3,000 / 5,000 m` geography buffer，再转换为 `MULTIPOLYGON,4326`。圆形研究窗口允许跨越海峡、马来西亚边界或海面；业务道路仍仅由 `ST_Intersects` 与实际 LTA Link 决定。

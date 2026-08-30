# CIQ Direction Relation Future Extension

当前 API01 范围只使用 `ST_Intersects`，同时保留进入关口和离开关口的 LTA Link，不新增 `TO_CIQ` / `FROM_CIQ` 字段，也不依赖未知方向属性过滤。

如未来取得可审计的道路方向、车道或路网拓扑数据，可在独立迁移与验证后扩展方向关系；该扩展不属于当前 API01 数据库写入门槛。

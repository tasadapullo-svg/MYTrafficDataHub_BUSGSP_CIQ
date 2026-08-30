package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：研究城市数据库实体。
 *
 * 功能：只读映射 core.study_city 以解析城市、时区和 Schema；
 * 输入来自冻结数据库，输出供 Feed 路由上下文使用；不承担任何 DDL 或业务表写入。
 */
@TableName("core.study_city")
public class CoreStudyCityEntity {
    @TableId(value = "uid", type = IdType.INPUT) public UUID uid;
    @TableField("city_code") public String cityCode;
    @TableField("city_name") public String cityName;
    @TableField("country_code") public String countryCode;
    @TableField("timezone_name") public String timezoneName;
    @TableField("schema_name") public String schemaName;
    @TableField("enabled") public Boolean enabled;
    @TableField("description") public String description;
    @TableField("create_time") public Instant createTime;
    @TableField("update_time") public Instant updateTime;
}

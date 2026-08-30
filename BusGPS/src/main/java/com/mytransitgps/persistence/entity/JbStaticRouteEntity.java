package com.mytransitgps.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.mytransitgps.persistence.typehandler.PostgreSqlJsonbTypeHandler;
import java.time.Instant;
import java.util.UUID;

/**
 * 中文名称：jb.static_route 数据库实体。
 *
 * 功能说明：严格按照已冻结的 PostgreSQL 16 实际列定义映射，
 * 仅用于 MyBatis-Plus 查询与持久化，不触发任何 Schema 自动变更。
 */
@TableName(value = "jb.static_route", autoResultMap = true)
public class JbStaticRouteEntity {

    @TableId(value = "uid", type = IdType.INPUT)
    public UUID uid;

    @TableField("static_version_uid")
    public UUID staticVersionUid;

    @TableField("route_id")
    public String routeId;

    @TableField("agency_id")
    public String agencyId;

    @TableField("route_short_name")
    public String routeShortName;

    @TableField("route_long_name")
    public String routeLongName;

    @TableField("route_desc")
    public String routeDesc;

    @TableField("route_type")
    public Integer routeType;

    @TableField("route_url")
    public String routeUrl;

    @TableField("route_color")
    public String routeColor;

    @TableField("route_text_color")
    public String routeTextColor;

    @TableField("route_sort_order")
    public Integer routeSortOrder;

    @TableField("continuous_pickup")
    public Integer continuousPickup;

    @TableField("continuous_drop_off")
    public Integer continuousDropOff;

    @TableField("network_id")
    public String networkId;

    @TableField(value = "source_row", typeHandler = PostgreSqlJsonbTypeHandler.class)
    public JsonNode sourceRow;

    @TableField("create_time")
    public Instant createTime;

}

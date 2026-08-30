package com.mytransitgps.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mytransitgps.persistence.entity.JbStaticTripEntity;

/**
 * 中文名称：jb.static_trip MyBatis-Plus Mapper。
 *
 * 功能说明：提供冻结表的基础查询与持久化入口；复杂批量和 UPSERT 由受控自定义 SQL 完成。
 */
public interface JbStaticTripMapper extends BaseMapper<JbStaticTripEntity> {
}
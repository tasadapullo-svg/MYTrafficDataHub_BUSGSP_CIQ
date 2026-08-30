package com.mytransitgps.persistence.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mytransitgps.persistence.entity.JbStaticRouteEntity;
import com.mytransitgps.persistence.entity.JbStaticShapeEntity;
import com.mytransitgps.persistence.entity.JbStaticStopEntity;
import com.mytransitgps.persistence.entity.JbStaticTripEntity;
import com.mytransitgps.persistence.entity.JbStaticVersionEntity;
import com.mytransitgps.persistence.mapper.JbStaticRouteMapper;
import com.mytransitgps.persistence.mapper.JbStaticShapeMapper;
import com.mytransitgps.persistence.mapper.JbStaticStopMapper;
import com.mytransitgps.persistence.mapper.JbStaticTripMapper;
import com.mytransitgps.persistence.mapper.JbStaticVersionMapper;
import com.mytransitgps.persistence.model.StaticReferenceIndex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中文名称：Johor Bahru Static UUID 动态解析服务。
 *
 * 功能说明：使用 feed_uid + static_sha256 定位版本，并按源 route/trip/stop/shape ID
 * 动态加载 UUID 外键，严禁硬编码任何数据库主键。
 */
@Service
@ConditionalOnProperty(prefix = "mytransitgps.database", name = "enabled", havingValue = "true")
public class StaticUidResolver {

    private static final Logger log = LoggerFactory.getLogger(StaticUidResolver.class);
    private final JbStaticVersionMapper versionMapper;
    private final JbStaticRouteMapper routeMapper;
    private final JbStaticTripMapper tripMapper;
    private final JbStaticStopMapper stopMapper;
    private final JbStaticShapeMapper shapeMapper;

    public StaticUidResolver(JbStaticVersionMapper versionMapper, JbStaticRouteMapper routeMapper,
                             JbStaticTripMapper tripMapper, JbStaticStopMapper stopMapper,
                             JbStaticShapeMapper shapeMapper) {
        this.versionMapper = versionMapper;
        this.routeMapper = routeMapper;
        this.tripMapper = tripMapper;
        this.stopMapper = stopMapper;
        this.shapeMapper = shapeMapper;
    }

    public JbStaticVersionEntity findVersion(UUID feedUid, String sha256) {
        return versionMapper.selectOne(new QueryWrapper<JbStaticVersionEntity>()
                .eq("feed_uid", feedUid).eq("static_sha256", sha256));
    }

    public StaticReferenceIndex load(UUID versionUid, String sha256) {
        Map<String, UUID> routes = mapRoutes(versionUid);
        Map<String, UUID> trips = mapTrips(versionUid);
        Map<String, UUID> stops = mapStops(versionUid);
        Map<String, UUID> shapes = mapShapes(versionUid);
        log.info("Static UUID索引加载完成，staticVersionUid={}，routes={}，trips={}，stops={}，shapes={}，sha256={}",
                versionUid, routes.size(), trips.size(), stops.size(), shapes.size(), sha256);
        return new StaticReferenceIndex(versionUid, sha256, routes, trips, stops, shapes);
    }

    private Map<String, UUID> mapRoutes(UUID versionUid) {
        Map<String, UUID> result = new LinkedHashMap<>();
        for (JbStaticRouteEntity row : routeMapper.selectList(new QueryWrapper<JbStaticRouteEntity>().eq("static_version_uid", versionUid))) {
            result.put(row.routeId, row.uid);
        }
        return Map.copyOf(result);
    }

    private Map<String, UUID> mapTrips(UUID versionUid) {
        Map<String, UUID> result = new LinkedHashMap<>();
        for (JbStaticTripEntity row : tripMapper.selectList(new QueryWrapper<JbStaticTripEntity>().eq("static_version_uid", versionUid))) {
            result.put(row.tripId, row.uid);
        }
        return Map.copyOf(result);
    }

    private Map<String, UUID> mapStops(UUID versionUid) {
        Map<String, UUID> result = new LinkedHashMap<>();
        for (JbStaticStopEntity row : stopMapper.selectList(new QueryWrapper<JbStaticStopEntity>().eq("static_version_uid", versionUid))) {
            result.put(row.stopId, row.uid);
        }
        return Map.copyOf(result);
    }

    private Map<String, UUID> mapShapes(UUID versionUid) {
        Map<String, UUID> result = new LinkedHashMap<>();
        for (JbStaticShapeEntity row : shapeMapper.selectList(new QueryWrapper<JbStaticShapeEntity>().eq("static_version_uid", versionUid))) {
            result.put(row.shapeId, row.uid);
        }
        return Map.copyOf(result);
    }
}

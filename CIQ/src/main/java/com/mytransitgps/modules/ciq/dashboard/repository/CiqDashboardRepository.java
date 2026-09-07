package com.mytransitgps.modules.ciq.dashboard.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/** 仅使用 SELECT 的 CIQ 大屏数据访问层。 */
@Repository
@ConditionalOnProperty(prefix="mytransitgps.database",name="enabled",havingValue="true")
public class CiqDashboardRepository {
    private final JdbcTemplate jdbc;

    public CiqDashboardRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ApiRow> apiRows() {
        String sql = """
                WITH run_stats AS (
                  SELECT api_endpoint_uid,
                         max(request_end_time) FILTER (WHERE success) AS last_success,
                         count(*) FILTER (WHERE request_start_time >= CURRENT_DATE) AS today_executions
                  FROM lta.collection_run
                  GROUP BY api_endpoint_uid
                )
                SELECT e.api_code,e.api_name,e.endpoint_url,
                       lr.uid,lr.request_start_time,lr.request_end_time,lr.http_status,lr.page_count,
                       lr.record_count,lr.response_bytes,lr.success,lr.snapshot_complete,lr.retry_count,lr.error_message,
                       rs.last_success,COALESCE(rs.today_executions,0),
                       CASE WHEN lr.uid IS NULL THEN NULL ELSE CASE e.api_code
                         WHEN 'API01' THEN (SELECT count(*) FROM lta.traffic_speed_observation x WHERE x.run_uid=lr.uid)
                         WHEN 'API02' THEN (SELECT count(*) FROM lta.travel_time_observation x WHERE x.run_uid=lr.uid)
                         WHEN 'API03' THEN (SELECT count(*) FROM lta.traffic_incident_event x WHERE x.first_run_uid=lr.uid)
                         WHEN 'API04' THEN (SELECT count(*) FROM lta.vms_message_state x WHERE x.first_run_uid=lr.uid)
                         WHEN 'API05' THEN (SELECT count(*) FROM lta.faulty_traffic_light_event x WHERE x.first_run_uid=lr.uid)
                         WHEN 'API06' THEN (SELECT count(*) FROM lta.road_work_event x WHERE x.first_run_uid=lr.uid)
                         WHEN 'API07' THEN (SELECT count(*) FROM lta.traffic_flow_file x WHERE x.run_uid=lr.uid)
                         WHEN 'API08' THEN (SELECT count(*) FROM lta.road_opening_event x WHERE x.first_run_uid=lr.uid)
                       END END db_inserted,
                       CASE e.api_code
                         WHEN 'API01' THEN (SELECT count(*) FROM lta.traffic_speed_observation x WHERE x.run_uid=lr.uid)
                         WHEN 'API02' THEN (SELECT count(*) FROM lta.travel_time_observation x JOIN lta.travel_time_segment seg ON seg.uid=x.segment_uid WHERE x.run_uid=lr.uid AND seg.area_uid IS NOT NULL)
                         WHEN 'API03' THEN (SELECT count(*) FROM lta.traffic_incident_event x WHERE x.last_run_uid=lr.uid AND x.area_uid IS NOT NULL)
                         WHEN 'API04' THEN (SELECT count(*) FROM lta.vms_equipment x WHERE x.area_uid IS NOT NULL AND x.last_seen_time BETWEEN lr.request_start_time-interval '1 minute' AND COALESCE(lr.request_end_time,CURRENT_TIMESTAMP)+interval '1 minute')
                         WHEN 'API05' THEN (SELECT count(*) FROM lta.faulty_traffic_light_event x WHERE x.last_run_uid=lr.uid AND x.area_uid IS NOT NULL)
                         WHEN 'API06' THEN (SELECT count(*) FROM lta.road_work_event x WHERE x.last_run_uid=lr.uid AND x.area_uid IS NOT NULL)
                         WHEN 'API08' THEN (SELECT count(*) FROM lta.road_opening_event x WHERE x.last_run_uid=lr.uid AND x.area_uid IS NOT NULL)
                       END ciq_selected,
                       a.file_name,a.file_size_bytes,a.sha256,
                       COALESCE(a.artifact_count,0) artifact_count
                FROM lta.api_endpoint e
                LEFT JOIN run_stats rs ON rs.api_endpoint_uid=e.uid
                LEFT JOIN LATERAL (SELECT r.* FROM lta.collection_run r WHERE r.api_endpoint_uid=e.uid ORDER BY r.request_start_time DESC LIMIT 1) lr ON true
                LEFT JOIN LATERAL (
                    SELECT max(ca.file_name) FILTER (WHERE ca.rn=1) file_name,
                           max(ca.file_size_bytes) FILTER (WHERE ca.rn=1) file_size_bytes,
                           max(ca.sha256) FILTER (WHERE ca.rn=1) sha256,
                           count(*) artifact_count
                    FROM (SELECT x.*,row_number() OVER(ORDER BY x.create_time DESC) rn FROM lta.collection_artifact x WHERE x.run_uid=lr.uid) ca
                ) a ON true
                ORDER BY e.api_code
                """;
        return jdbc.query(sql, (rs, n) -> apiRow(rs));
    }

    /**
     * API01-API08 数据层摘要。latest/today 均为业务表真实聚合；total 使用 PostgreSQL
     * pg_stat_user_tables 的 n_live_tup，避免首页对超大时序表执行全表 COUNT(*)。
     */
    public List<ApiDataRow> apiDataRows() {
        String sql = """
                WITH table_stats AS (
                  SELECT relname, n_live_tup::bigint AS total_count
                  FROM pg_stat_user_tables
                  WHERE schemaname='lta'
                )
                SELECT * FROM (
                  SELECT 'API01' api_code,
                         (SELECT max(snapshot_time) FROM lta.traffic_speed_observation) latest_data_time,
                         (SELECT count(*) FROM lta.traffic_speed_observation WHERE snapshot_time>=CURRENT_DATE) today_count,
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='traffic_speed_observation'),0) total_count
                  UNION ALL
                  SELECT 'API02',
                         (SELECT max(snapshot_time) FROM lta.travel_time_observation),
                         (SELECT count(*) FROM lta.travel_time_observation WHERE snapshot_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='travel_time_observation'),0)
                  UNION ALL
                  SELECT 'API03',
                         (SELECT max(last_seen_time) FROM lta.traffic_incident_event),
                         (SELECT count(*) FROM lta.traffic_incident_event WHERE first_seen_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='traffic_incident_event'),0)
                  UNION ALL
                  SELECT 'API04',
                         (SELECT max(last_seen_time) FROM lta.vms_message_state),
                         (SELECT count(*) FROM lta.vms_message_state WHERE first_seen_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='vms_message_state'),0)
                  UNION ALL
                  SELECT 'API05',
                         (SELECT max(last_seen_time) FROM lta.faulty_traffic_light_event),
                         (SELECT count(*) FROM lta.faulty_traffic_light_event WHERE first_seen_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='faulty_traffic_light_event'),0)
                  UNION ALL
                  SELECT 'API06',
                         (SELECT max(last_seen_time) FROM lta.road_work_event),
                         (SELECT count(*) FROM lta.road_work_event WHERE first_seen_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='road_work_event'),0)
                  UNION ALL
                  SELECT 'API07',
                         (SELECT max(downloaded_time) FROM lta.traffic_flow_file),
                         (SELECT count(*) FROM lta.traffic_flow_file WHERE downloaded_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='traffic_flow_file'),0)
                  UNION ALL
                  SELECT 'API08',
                         (SELECT max(last_seen_time) FROM lta.road_opening_event),
                         (SELECT count(*) FROM lta.road_opening_event WHERE first_seen_time>=CURRENT_DATE),
                         COALESCE((SELECT total_count FROM table_stats WHERE relname='road_opening_event'),0)
                ) data
                ORDER BY api_code
                """;
        return jdbc.query(sql,(rs,n)->new ApiDataRow(rs.getString(1),instant(rs,2),rs.getLong(3),rs.getLong(4)));
    }

    public long countTodayRuns(Boolean success) {
        String condition = success == null ? "" : " AND success=" + success;
        Long value = jdbc.queryForObject("SELECT count(*) FROM lta.collection_run WHERE request_start_time>=CURRENT_DATE" + condition, Long.class);
        return value == null ? 0 : value;
    }

    public List<Map<String, Object>> trend(int hours) {
        return jdbc.queryForList("""
                SELECT e.api_code,
                       date_bin(interval '30 minutes',r.request_start_time,timestamptz '2000-01-01 00:00:00+00') bucket_time,
                       count(*) executions,sum(r.record_count) records
                FROM lta.collection_run r JOIN lta.api_endpoint e ON e.uid=r.api_endpoint_uid
                WHERE r.request_start_time>=CURRENT_TIMESTAMP-(? * interval '1 hour')
                GROUP BY e.api_code,bucket_time ORDER BY bucket_time,e.api_code
                """, hours);
    }

    public List<RequestRow> recentRequests(int limit) {
        return jdbc.query("""
                SELECT r.uid,r.request_start_time,r.request_end_time,e.api_code,e.api_name,r.http_status,r.record_count,
                       r.retry_count,r.success,r.snapshot_complete,
                       (SELECT count(*) FROM lta.collection_artifact a WHERE a.run_uid=r.uid AND a.sha256 IS NOT NULL) hashed_artifacts,
                       CASE e.api_code
                         WHEN 'API01' THEN (SELECT count(*) FROM lta.traffic_speed_observation x WHERE x.run_uid=r.uid)
                         WHEN 'API02' THEN (SELECT count(*) FROM lta.travel_time_observation x WHERE x.run_uid=r.uid)
                         WHEN 'API03' THEN (SELECT count(*) FROM lta.traffic_incident_event x WHERE x.first_run_uid=r.uid)
                         WHEN 'API04' THEN (SELECT count(*) FROM lta.vms_message_state x WHERE x.first_run_uid=r.uid)
                         WHEN 'API05' THEN (SELECT count(*) FROM lta.faulty_traffic_light_event x WHERE x.first_run_uid=r.uid)
                         WHEN 'API06' THEN (SELECT count(*) FROM lta.road_work_event x WHERE x.first_run_uid=r.uid)
                         WHEN 'API07' THEN (SELECT count(*) FROM lta.traffic_flow_file x WHERE x.run_uid=r.uid)
                         WHEN 'API08' THEN (SELECT count(*) FROM lta.road_opening_event x WHERE x.first_run_uid=r.uid)
                       END inserted
                FROM lta.collection_run r JOIN lta.api_endpoint e ON e.uid=r.api_endpoint_uid
                ORDER BY r.request_start_time DESC LIMIT ?
                """, (rs, n) -> requestRow(rs), limit);
    }

    public List<MapPointRow> mapPoints(String apiCode, int limit) {
        String code=apiCode==null?"ALL":apiCode.toUpperCase();
        int safe=Math.max(1,Math.min(limit,3000));
        List<MapPointRow> rows=new ArrayList<>();
        int each="ALL".equals(code)?Math.max(1,safe/2):safe;
        if("ALL".equals(code)||"API01".equals(code)) rows.addAll(api01MapPoints(each));
        if("ALL".equals(code)||"API02".equals(code)) rows.addAll(api02MapPoints(each));
        return rows.size()<=safe?rows:rows.subList(0,safe);
    }

    private List<MapPointRow> api01MapPoints(int limit) {
        return jdbc.query("""
                WITH latest_run AS (
                  SELECT r.uid FROM lta.collection_run r JOIN lta.api_endpoint e ON e.uid=r.api_endpoint_uid
                  WHERE e.api_code='API01' AND r.success AND r.snapshot_complete
                  ORDER BY r.request_start_time DESC LIMIT 1
                )
                SELECT DISTINCT ON (l.uid) l.link_id,COALESCE(l.road_name,'Traffic Link'),
                       'Speed Band '||o.speed_band,(l.start_lat+l.end_lat)/2.0,(l.start_lon+l.end_lon)/2.0,
                       o.snapshot_time,a.ciq_code,a.area_name,o.speed_band,o.minimum_speed,o.maximum_speed,
                       degrees(ST_Azimuth(ST_MakePoint(l.start_lon,l.start_lat),ST_MakePoint(l.end_lon,l.end_lat))) bearing
                FROM lta.traffic_speed_observation o JOIN latest_run r ON r.uid=o.run_uid
                JOIN lta.traffic_link l ON l.uid=o.link_uid
                JOIN lta.traffic_link_scope s ON s.link_uid=l.uid AND s.active
                JOIN lta.study_area a ON a.uid=s.area_uid AND a.active
                ORDER BY l.uid,s.is_primary DESC,a.zone_code LIMIT ?
                """,(rs,n)->{
                    Double bearing=(Double)rs.getObject(12);
                    Map<String,Object> d=new LinkedHashMap<>();d.put("CIQ",rs.getString(7));d.put("Study Area",rs.getString(8));d.put("Speed Band",rs.getInt(9));d.put("Minimum Speed",integer(rs,10));d.put("Maximum Speed",integer(rs,11));
                    if(bearing!=null){double normalized=(bearing+360.0)%360.0;d.put("Direction",compass(normalized));d.put("Bearing Degrees",Math.round(normalized));}
                    d.put("Location Basis","Traffic link midpoint");
                    return new MapPointRow("API01",rs.getString(1),rs.getString(2),rs.getString(3),rs.getDouble(4),rs.getDouble(5),instant(rs,6),d);
                },limit);
    }

    private List<MapPointRow> api02MapPoints(int limit) {
        return jdbc.query("""
                WITH latest_run AS (
                  SELECT r.uid FROM lta.collection_run r JOIN lta.api_endpoint e ON e.uid=r.api_endpoint_uid
                  WHERE e.api_code='API02' AND r.success ORDER BY r.request_start_time DESC LIMIT 1
                ), stats AS (
                  SELECT count(*) segment_count,round(avg(o.est_time_min),1) avg_minutes,min(o.est_time_min) min_minutes,
                         max(o.est_time_min) max_minutes,max(o.snapshot_time) snapshot_time
                  FROM lta.travel_time_observation o JOIN latest_run r ON r.uid=o.run_uid
                ), areas AS (
                  SELECT DISTINCT ON (ciq_code) uid,ciq_code,area_name,geom FROM lta.study_area
                  WHERE active ORDER BY ciq_code,CASE WHEN zone_code='CORE' THEN 0 ELSE 1 END,area_name
                )
                SELECT a.uid::text,a.ciq_code,a.area_name,s.segment_count,s.avg_minutes,s.min_minutes,s.max_minutes,
                       s.snapshot_time,ST_Y(ST_PointOnSurface(a.geom)),ST_X(ST_PointOnSurface(a.geom))
                FROM areas a CROSS JOIN stats s WHERE s.segment_count>0 ORDER BY a.ciq_code LIMIT ?
                """,(rs,n)->{
                    Map<String,Object> d=new LinkedHashMap<>();d.put("CIQ",rs.getString(2));d.put("Study Area",rs.getString(3));d.put("Segments",rs.getLong(4));d.put("Average Minutes",rs.getBigDecimal(5));d.put("Minimum Minutes",rs.getInt(6));d.put("Maximum Minutes",rs.getInt(7));d.put("Location Basis","Study-area reference only; API02 payload has no coordinates");
                    return new MapPointRow("API02",rs.getString(1),"API02 · "+rs.getString(2)+" travel-time snapshot",rs.getLong(4)+" segments · avg "+rs.getBigDecimal(5)+" min",rs.getDouble(9),rs.getDouble(10),instant(rs,8),d);
                },limit);
    }

    public List<DataRecordRow> dataRecords(String apiCode, Instant from, Instant to, String category, int limit) {
        String code=apiCode==null?"ALL":apiCode.toUpperCase();
        int safe=Math.max(1,Math.min(limit,120));
        StringBuilder sql=new StringBuilder("""
                WITH records AS (
                  SELECT 'API03' api_code,uid::text record_id,type category,type title,message description,last_seen_time event_time,
                         CASE WHEN active THEN 'ACTIVE' ELSE 'RESOLVED' END status,latitude::double precision latitude,longitude::double precision longitude,
                         'First seen' d1_label,first_seen_time::text d1_value,'Resolved' d2_label,resolved_time::text d2_value
                  FROM lta.traffic_incident_event
                  UNION ALL
                  SELECT 'API04',m.uid::text,'VMS / EMAS',e.equipment_id,m.message,m.last_seen_time,
                         CASE WHEN m.active THEN 'ACTIVE' ELSE 'ENDED' END,e.latitude::double precision,e.longitude::double precision,
                         'Equipment' ,e.equipment_id,'First seen',m.first_seen_time::text
                  FROM lta.vms_message_state m JOIN lta.vms_equipment e ON e.uid=m.equipment_uid
                  UNION ALL
                  SELECT 'API05',uid::text,COALESCE(fault_type::text,'UNCLASSIFIED'),node_id,COALESCE(message,'No message'),last_seen_time,
                         CASE WHEN active THEN 'ACTIVE' ELSE 'RESOLVED' END,NULL::double precision,NULL::double precision,
                         'Alarm',alarm_id,'Source start',source_start_time::text FROM lta.faulty_traffic_light_event
                  UNION ALL
                  SELECT 'API06',uid::text,COALESCE(svc_dept,'ROAD WORK'),COALESCE(road_name,event_id),COALESCE(other,'Approved road work'),last_seen_time,
                         CASE WHEN active THEN 'ACTIVE' ELSE 'ENDED' END,NULL::double precision,NULL::double precision,
                         'Start date',start_date::text,'End date',end_date::text FROM lta.road_work_event
                  UNION ALL
                  SELECT 'API07',uid::text,parse_status,file_name,COALESCE(source_period,'Traffic flow file'),downloaded_time,
                         parse_status,NULL::double precision,NULL::double precision,'File size',file_size_bytes::text,'SHA-256',sha256 FROM lta.traffic_flow_file
                  UNION ALL
                  SELECT 'API08',uid::text,COALESCE(svc_dept,'ROAD OPENING'),COALESCE(road_name,event_id),COALESCE(other,'Planned road opening'),last_seen_time,
                         CASE WHEN active THEN 'ACTIVE' ELSE 'ENDED' END,NULL::double precision,NULL::double precision,
                         'Start date',start_date::text,'End date',end_date::text FROM lta.road_opening_event
                ), filtered AS (SELECT * FROM records WHERE 1=1
                """);
        List<Object> args=new ArrayList<>();
        if(!"ALL".equals(code)){sql.append(" AND api_code=?");args.add(code);}
        if(from!=null){sql.append(" AND event_time>=?");args.add(Timestamp.from(from));}
        if(to!=null){sql.append(" AND event_time<=?");args.add(Timestamp.from(to));}
        if(category!=null&&!category.isBlank()){sql.append(" AND (category ILIKE ? OR title ILIKE ?)");String q="%"+category.trim()+"%";args.add(q);args.add(q);}
        sql.append("), ranked AS (SELECT *,row_number() OVER(PARTITION BY api_code ORDER BY event_time DESC NULLS LAST) rn FROM filtered) SELECT * FROM ranked");
        if("ALL".equals(code)){sql.append(" WHERE rn<=?");args.add(Math.max(1,(int)Math.ceil(safe/6.0)));}
        sql.append(" ORDER BY event_time DESC NULLS LAST LIMIT ?");args.add(safe);
        return jdbc.query(sql.toString(),(rs,n)->{
            Map<String,Object> d=new LinkedHashMap<>();if(rs.getString("d1_label")!=null)d.put(rs.getString("d1_label"),rs.getString("d1_value"));if(rs.getString("d2_label")!=null)d.put(rs.getString("d2_label"),rs.getString("d2_value"));
            return new DataRecordRow(rs.getString("api_code"),rs.getString("record_id"),rs.getString("category"),rs.getString("title"),rs.getString("description"),instant(rs,6),rs.getString("status"),(Double)rs.getObject("latitude"),(Double)rs.getObject("longitude"),d);
        },args.toArray());
    }


    public StorageRow storage() {
        return jdbc.queryForObject("""
                SELECT count(*) FILTER(WHERE a.create_time>=CURRENT_DATE),count(*),
                       COALESCE(sum(a.file_size_bytes) FILTER(WHERE a.create_time>=CURRENT_DATE),0),COALESCE(sum(a.file_size_bytes),0),
                       count(*) FILTER(WHERE a.compression_type<>'NONE'),max(a.create_time),
                       (SELECT COALESCE(sum(n_live_tup),0) FROM pg_stat_user_tables WHERE schemaname='lta'),
                       (SELECT COALESCE(sum(n_tup_ins),0) FROM pg_stat_user_tables WHERE schemaname='lta'),
                       (SELECT max(t) FROM (SELECT max(create_time) t FROM lta.traffic_speed_observation UNION ALL SELECT max(create_time) FROM lta.travel_time_observation UNION ALL SELECT max(create_time) FROM lta.traffic_incident_event UNION ALL SELECT max(create_time) FROM lta.vms_message_state) z)
                FROM lta.collection_artifact a
                """, (rs,n)->new StorageRow(rs.getLong(1),rs.getLong(2),rs.getLong(3),rs.getLong(4),rs.getLong(5),instant(rs,6),rs.getLong(7),rs.getLong(8),instant(rs,9)));
    }

    private static ApiRow apiRow(ResultSet r) throws SQLException { return new ApiRow(r.getString(1),r.getString(2),r.getString(3),(UUID)r.getObject(4),instant(r,5),instant(r,6),integer(r,7),integer(r,8),longValue(r,9),longValue(r,10),(Boolean)r.getObject(11),(Boolean)r.getObject(12),integer(r,13),r.getString(14),instant(r,15),r.getLong(16),longValue(r,17),longValue(r,18),r.getString(19),longValue(r,20),r.getString(21),r.getLong(22)); }
    private static RequestRow requestRow(ResultSet r)throws SQLException{return new RequestRow((UUID)r.getObject(1),instant(r,2),instant(r,3),r.getString(4),r.getString(5),integer(r,6),longValue(r,7),integer(r,8),r.getBoolean(9),r.getBoolean(10),r.getLong(11),longValue(r,12));}
    private static Instant instant(ResultSet r,int i)throws SQLException{Timestamp t=r.getTimestamp(i);return t==null?null:t.toInstant();}
    private static Integer integer(ResultSet r,int i)throws SQLException{Object v=r.getObject(i);return v instanceof Number n?n.intValue():null;}
    private static Long longValue(ResultSet r,int i)throws SQLException{Object v=r.getObject(i);return v instanceof Number n?n.longValue():null;}
    private static String compass(double bearing){String[] directions={"N","NE","E","SE","S","SW","W","NW"};return directions[(int)Math.floor((bearing+22.5)/45.0)%8];}

    public record ApiRow(String code,String name,String endpoint,UUID runUid,Instant start,Instant end,Integer http,Integer pages,Long raw,Long bytes,Boolean success,Boolean complete,Integer retries,String error,Instant lastSuccess,long todayExecutions,Long inserted,Long selected,String fileName,Long fileSize,String sha,long artifactCount){}
    public record ApiDataRow(String code,Instant latestDataTime,long todayCount,long totalCount){}
    public record RequestRow(UUID uid,Instant start,Instant end,String code,String name,Integer http,Long raw,Integer retry,boolean success,boolean complete,long hashedArtifacts,Long inserted){}
    public record MapPointRow(String apiCode,String recordId,String title,String category,double latitude,double longitude,Instant observedAt,Map<String,Object> details){}
    public record DataRecordRow(String apiCode,String recordId,String category,String title,String description,Instant eventTime,String status,Double latitude,Double longitude,Map<String,Object> details){}
    public record StorageRow(long todayFiles,long totalFiles,long todayBytes,long totalBytes,long archiveCount,Instant latestArchive,long dbTotal,long dbInserted,Instant latestDb){}
}

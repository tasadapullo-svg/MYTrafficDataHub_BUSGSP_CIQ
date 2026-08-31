-- Required reference configuration for a new MYTrafficDataHub production database.
-- Source: current development database. Contains no observations, runs, logs, or historical business data.
BEGIN;

INSERT INTO core.study_city (uid,city_code,city_name,schema_name,country_code,timezone_name,enabled,description)
VALUES ('d5f3dfce-5e49-482a-8ce5-12be102a3e42'::uuid,'johor_bahru','Johor Bahru','jb','MY','Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (city_code) DO UPDATE SET city_name=EXCLUDED.city_name,schema_name=EXCLUDED.schema_name,country_code=EXCLUDED.country_code,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.study_city (uid,city_code,city_name,schema_name,country_code,timezone_name,enabled,description)
VALUES ('dbf9d6aa-b224-4748-a317-0c51421ede3f'::uuid,'kuala_lumpur','Kuala Lumpur','kl','MY','Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (city_code) DO UPDATE SET city_name=EXCLUDED.city_name,schema_name=EXCLUDED.schema_name,country_code=EXCLUDED.country_code,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.study_city (uid,city_code,city_name,schema_name,country_code,timezone_name,enabled,description)
VALUES ('43ad10eb-8016-47ac-8c97-b363e784f595'::uuid,'kuching','Kuching','kuching','MY','Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (city_code) DO UPDATE SET city_name=EXCLUDED.city_name,schema_name=EXCLUDED.schema_name,country_code=EXCLUDED.country_code,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.study_city (uid,city_code,city_name,schema_name,country_code,timezone_name,enabled,description)
VALUES ('edb73471-208c-4802-a5c0-29e38f4c12bd'::uuid,'melaka','Melaka','melaka','MY','Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (city_code) DO UPDATE SET city_name=EXCLUDED.city_name,schema_name=EXCLUDED.schema_name,country_code=EXCLUDED.country_code,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.gtfs_feed (uid,city_uid,feed_id,feed_name,operator_name,service_type,realtime_url,static_url,realtime_feed_type,source_platform,file_prefix,poll_interval_seconds,stagger_offset_seconds,timezone_name,enabled,description)
VALUES ('86671fa7-aac9-4309-9154-91817b0b83a9'::uuid,(SELECT uid FROM core.study_city WHERE city_code='johor_bahru'),'mybas-johor','Johor Bahru BAS.MY Vehicle Position','BAS.MY','bus','https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-johor/','https://api.data.gov.my/gtfs-static/mybas-johor','vehicle_position','data.gov.my','johor_bahru',120,0,'Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (feed_id) DO UPDATE SET city_uid=EXCLUDED.city_uid,feed_name=EXCLUDED.feed_name,operator_name=EXCLUDED.operator_name,service_type=EXCLUDED.service_type,realtime_url=EXCLUDED.realtime_url,static_url=EXCLUDED.static_url,realtime_feed_type=EXCLUDED.realtime_feed_type,source_platform=EXCLUDED.source_platform,file_prefix=EXCLUDED.file_prefix,poll_interval_seconds=EXCLUDED.poll_interval_seconds,stagger_offset_seconds=EXCLUDED.stagger_offset_seconds,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.gtfs_feed (uid,city_uid,feed_id,feed_name,operator_name,service_type,realtime_url,static_url,realtime_feed_type,source_platform,file_prefix,poll_interval_seconds,stagger_offset_seconds,timezone_name,enabled,description)
VALUES ('16c43968-04de-4126-8a6f-37509de4452e'::uuid,(SELECT uid FROM core.study_city WHERE city_code='kuching'),'mybas-kuching','Kuching BAS.MY Vehicle Position','BAS.MY','bus','https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-kuching/','https://api.data.gov.my/gtfs-static/mybas-kuching','vehicle_position','data.gov.my','kuching',120,24,'Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (feed_id) DO UPDATE SET city_uid=EXCLUDED.city_uid,feed_name=EXCLUDED.feed_name,operator_name=EXCLUDED.operator_name,service_type=EXCLUDED.service_type,realtime_url=EXCLUDED.realtime_url,static_url=EXCLUDED.static_url,realtime_feed_type=EXCLUDED.realtime_feed_type,source_platform=EXCLUDED.source_platform,file_prefix=EXCLUDED.file_prefix,poll_interval_seconds=EXCLUDED.poll_interval_seconds,stagger_offset_seconds=EXCLUDED.stagger_offset_seconds,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.gtfs_feed (uid,city_uid,feed_id,feed_name,operator_name,service_type,realtime_url,static_url,realtime_feed_type,source_platform,file_prefix,poll_interval_seconds,stagger_offset_seconds,timezone_name,enabled,description)
VALUES ('b125c11e-d463-477d-84a3-15870552ac1c'::uuid,(SELECT uid FROM core.study_city WHERE city_code='melaka'),'mybas-melaka','Melaka BAS.MY Vehicle Position','BAS.MY','bus','https://api.data.gov.my/gtfs-realtime/vehicle-position/mybas-melaka/','https://api.data.gov.my/gtfs-static/mybas-melaka','vehicle_position','data.gov.my','melaka',120,96,'Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (feed_id) DO UPDATE SET city_uid=EXCLUDED.city_uid,feed_name=EXCLUDED.feed_name,operator_name=EXCLUDED.operator_name,service_type=EXCLUDED.service_type,realtime_url=EXCLUDED.realtime_url,static_url=EXCLUDED.static_url,realtime_feed_type=EXCLUDED.realtime_feed_type,source_platform=EXCLUDED.source_platform,file_prefix=EXCLUDED.file_prefix,poll_interval_seconds=EXCLUDED.poll_interval_seconds,stagger_offset_seconds=EXCLUDED.stagger_offset_seconds,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.gtfs_feed (uid,city_uid,feed_id,feed_name,operator_name,service_type,realtime_url,static_url,realtime_feed_type,source_platform,file_prefix,poll_interval_seconds,stagger_offset_seconds,timezone_name,enabled,description)
VALUES ('1210e2fb-1548-4984-a5da-5bd806459722'::uuid,(SELECT uid FROM core.study_city WHERE city_code='kuala_lumpur'),'rapid-bus-kl','Kuala Lumpur Rapid Bus Vehicle Position','Prasarana','rapid_bus','https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-kl','https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-kl','vehicle_position','data.gov.my','kuala_lumpur_rapid_bus',120,48,'Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (feed_id) DO UPDATE SET city_uid=EXCLUDED.city_uid,feed_name=EXCLUDED.feed_name,operator_name=EXCLUDED.operator_name,service_type=EXCLUDED.service_type,realtime_url=EXCLUDED.realtime_url,static_url=EXCLUDED.static_url,realtime_feed_type=EXCLUDED.realtime_feed_type,source_platform=EXCLUDED.source_platform,file_prefix=EXCLUDED.file_prefix,poll_interval_seconds=EXCLUDED.poll_interval_seconds,stagger_offset_seconds=EXCLUDED.stagger_offset_seconds,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO core.gtfs_feed (uid,city_uid,feed_id,feed_name,operator_name,service_type,realtime_url,static_url,realtime_feed_type,source_platform,file_prefix,poll_interval_seconds,stagger_offset_seconds,timezone_name,enabled,description)
VALUES ('7f89e04c-f644-4c27-8817-6b550adb30e4'::uuid,(SELECT uid FROM core.study_city WHERE city_code='kuala_lumpur'),'rapid-bus-mrtfeeder','Kuala Lumpur MRT Feeder Vehicle Position','Prasarana','mrt_feeder','https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-mrtfeeder','https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-mrtfeeder','vehicle_position','data.gov.my','kuala_lumpur_mrt_feeder',120,72,'Asia/Kuala_Lumpur',TRUE,NULL)
ON CONFLICT (feed_id) DO UPDATE SET city_uid=EXCLUDED.city_uid,feed_name=EXCLUDED.feed_name,operator_name=EXCLUDED.operator_name,service_type=EXCLUDED.service_type,realtime_url=EXCLUDED.realtime_url,static_url=EXCLUDED.static_url,realtime_feed_type=EXCLUDED.realtime_feed_type,source_platform=EXCLUDED.source_platform,file_prefix=EXCLUDED.file_prefix,poll_interval_seconds=EXCLUDED.poll_interval_seconds,stagger_offset_seconds=EXCLUDED.stagger_offset_seconds,timezone_name=EXCLUDED.timezone_name,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('e90eca07-b6e0-418b-83f2-0f67fe0c87f7'::uuid,'API01','TrafficSpeedBands','https://datamall2.mytransport.sg/ltaodataservice/v4/TrafficSpeedBands','CRON',NULL,'0 10,20,30,50 * * * *',TRUE,TRUE,TRUE,'道路分段速度等级采集，用于CIQ交通状态与预测研究。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('0867fa9c-1870-4c5e-a00b-05bd46534371'::uuid,'API02','Estimated Travel Times','https://datamall2.mytransport.sg/ltaodataservice/EstTravelTimes','MINUTES',15,NULL,TRUE,TRUE,TRUE,'高速公路分段预计旅行时间采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('cf3e4474-2f8c-4451-807c-6afeb8f2355d'::uuid,'API03','Traffic Incidents','https://datamall2.mytransport.sg/ltaodataservice/TrafficIncidents','MINUTES',5,NULL,TRUE,TRUE,TRUE,'实时交通事件生命周期采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('ac7b1c97-f986-4375-9d33-023621e0bd50'::uuid,'API04','VMS / EMAS','https://datamall2.mytransport.sg/ltaodataservice/VMS','MINUTES',5,NULL,TRUE,TRUE,TRUE,'电子信息板设备与消息状态采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('e6ab5423-3331-4085-96d5-bce33826fc61'::uuid,'API05','Faulty Traffic Lights','https://datamall2.mytransport.sg/ltaodataservice/FaultyTrafficLights','HOURLY',60,NULL,TRUE,TRUE,TRUE,'故障交通信号灯事件采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('2ef28653-e967-408e-9524-fa50ee69684b'::uuid,'API06','Approved Road Works','https://datamall2.mytransport.sg/ltaodataservice/RoadWorks','DAILY',1440,NULL,TRUE,TRUE,TRUE,'已批准道路施工计划采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('8c08f25b-94c8-41ff-b79b-9d06f7123308'::uuid,'API07','Traffic Flow','https://datamall2.mytransport.sg/ltaodataservice/TrafficFlow','MONTH_END',NULL,'LAST_DAY_OF_MONTH',TRUE,TRUE,TRUE,'每月最后一天获取历史交通流量下载文件。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

INSERT INTO lta.api_endpoint (uid,api_code,api_name,endpoint_url,schedule_type,interval_minutes,schedule_rule,raw_save_enabled,db_write_enabled,enabled,description)
VALUES ('aa9a1412-c91e-4b59-8c09-f9e81c4d0372'::uuid,'API08','Planned Road Openings','https://datamall2.mytransport.sg/ltaodataservice/RoadOpenings','DAILY',1440,NULL,TRUE,TRUE,TRUE,'计划道路开放与网络调整事件采集。')
ON CONFLICT (api_code) DO UPDATE SET api_name=EXCLUDED.api_name,endpoint_url=EXCLUDED.endpoint_url,schedule_type=EXCLUDED.schedule_type,interval_minutes=EXCLUDED.interval_minutes,schedule_rule=EXCLUDED.schedule_rule,raw_save_enabled=EXCLUDED.raw_save_enabled,db_write_enabled=EXCLUDED.db_write_enabled,enabled=EXCLUDED.enabled,description=EXCLUDED.description,update_time=CURRENT_TIMESTAMP;

COMMIT;

SELECT 'core.study_city' AS table_name,count(*) AS row_count FROM core.study_city
UNION ALL SELECT 'core.gtfs_feed',count(*) FROM core.gtfs_feed
UNION ALL SELECT 'lta.api_endpoint',count(*) FROM lta.api_endpoint
ORDER BY table_name;

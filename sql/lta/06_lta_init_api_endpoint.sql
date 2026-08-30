INSERT INTO lta.api_endpoint
    (api_code, api_name, endpoint_url, schedule_type, interval_minutes, schedule_rule, description)
VALUES
    ('API01', 'TrafficSpeedBands', 'https://datamall2.mytransport.sg/ltaodataservice/v4/TrafficSpeedBands', 'CRON', NULL, '0 0,15,30,45 * * * *', '道路分段速度等级采集，用于CIQ交通状态与预测研究。'),
    ('API02', 'Estimated Travel Times', 'https://datamall2.mytransport.sg/ltaodataservice/EstTravelTimes', 'MINUTES', 15, NULL, '高速公路分段预计旅行时间采集。'),
    ('API03', 'Traffic Incidents', 'https://datamall2.mytransport.sg/ltaodataservice/TrafficIncidents', 'MINUTES', 5, NULL, '实时交通事件生命周期采集。'),
    ('API04', 'VMS / EMAS', 'https://datamall2.mytransport.sg/ltaodataservice/VMS', 'MINUTES', 5, NULL, '电子信息板设备与消息状态采集。'),
    ('API05', 'Faulty Traffic Lights', 'https://datamall2.mytransport.sg/ltaodataservice/FaultyTrafficLights', 'HOURLY', 60, NULL, '故障交通信号灯事件采集。'),
    ('API06', 'Approved Road Works', 'https://datamall2.mytransport.sg/ltaodataservice/RoadWorks', 'DAILY', 1440, NULL, '已批准道路施工计划采集。'),
    ('API07', 'Traffic Flow', 'https://datamall2.mytransport.sg/ltaodataservice/TrafficFlow', 'MONTH_END', NULL, 'LAST_DAY_OF_MONTH', '每月最后一天获取历史交通流量下载文件。'),
    ('API08', 'Planned Road Openings', 'https://datamall2.mytransport.sg/ltaodataservice/RoadOpenings', 'DAILY', 1440, NULL, '计划道路开放与网络调整事件采集。')
ON CONFLICT (api_code) DO UPDATE SET
    api_name = EXCLUDED.api_name,
    endpoint_url = EXCLUDED.endpoint_url,
    schedule_type = EXCLUDED.schedule_type,
    interval_minutes = EXCLUDED.interval_minutes,
    schedule_rule = EXCLUDED.schedule_rule,
    description = EXCLUDED.description;

package com.mytransitgps.modules.ciq.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** API07 Traffic Flow 原始文件版本元数据持久化。 */
@Component
@ConditionalOnProperty(prefix = "traffic.ciq.persistence", name = "database-write-enabled", havingValue = "true")
public class TrafficFlowPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(TrafficFlowPersistenceService.class);

    private final JdbcTemplate jdbc;

    public TrafficFlowPersistenceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int persist(UUID runUid,
                       LocalDate month,
                       String downloadUrl,
                       String fileName,
                       String filePath,
                       long fileSize,
                       String sha256,
                       Instant downloadedTime) {
        LocalDate checkMonth = month.withDayOfMonth(1);
        log.info("CIQ API07 [DB-P01] TrafficFlow文件元数据持久化开始，runUid={}，checkMonth={}，fileName={}，sizeBytes={}，sha256={}",
                runUid, checkMonth, fileName, fileSize, sha256);

        int affected = jdbc.update("""
                INSERT INTO lta.traffic_flow_file(
                    uid,check_month,run_uid,download_url,file_name,file_path,file_size_bytes,sha256,
                    is_new_version,source_period,downloaded_time,parse_status
                )
                VALUES(gen_random_uuid(),?,?,?,?,?,?,?,TRUE,?,?, 'NOT_PARSED')
                ON CONFLICT(check_month) DO UPDATE SET
                    run_uid=EXCLUDED.run_uid,
                    download_url=EXCLUDED.download_url,
                    file_name=EXCLUDED.file_name,
                    file_path=EXCLUDED.file_path,
                    file_size_bytes=EXCLUDED.file_size_bytes,
                    is_new_version=(lta.traffic_flow_file.sha256 IS DISTINCT FROM EXCLUDED.sha256),
                    sha256=EXCLUDED.sha256,
                    source_period=EXCLUDED.source_period,
                    downloaded_time=EXCLUDED.downloaded_time,
                    update_time=CURRENT_TIMESTAMP
                """,
                checkMonth, runUid, downloadUrl, fileName, filePath, fileSize, sha256,
                month.toString(), Timestamp.from(downloadedTime));

        log.info("CIQ API07 [DB-P02] traffic_flow_file UPSERT完成，runUid={}，checkMonth={}，affected={}",
                runUid, checkMonth, affected);
        log.info("CIQ API07 [DB-P03] TrafficFlow文件元数据持久化完成，runUid={}，filePath={}，parseStatus=NOT_PARSED",
                runUid, filePath);
        return affected;
    }
}

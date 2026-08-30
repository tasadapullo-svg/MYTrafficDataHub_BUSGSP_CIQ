package com.mytransitgps.modules.ciq.persistence;
import com.mytransitgps.modules.ciq.storage.CiqRawJsonArtifact;import java.sql.Timestamp;import java.time.Instant;import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Component;
/** CIQ API02-08 共用只负责 collection_run/page/artifact 的审计写入。 */
@Component
@ConditionalOnProperty(prefix="traffic.ciq",name="enabled",havingValue="true")
@ConditionalOnProperty(prefix="traffic.ciq.persistence",name="database-write-enabled",havingValue="true")
public class CiqAuditPersistenceService {
 private final JdbcTemplate jdbc; public CiqAuditPersistenceService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public UUID endpointUid(String code){return jdbc.queryForObject("SELECT uid FROM lta.api_endpoint WHERE api_code=? AND enabled=TRUE",UUID.class,code);}
 public void begin(UUID run,String code,String mode,Instant scheduled,Instant start){jdbc.update("INSERT INTO lta.collection_run(uid,api_endpoint_uid,run_mode,scheduled_time,request_start_time,success,snapshot_complete) VALUES(?,?,?,?,?,FALSE,FALSE)",run,endpointUid(code),mode,ts(scheduled),ts(start));}
 public void page(UUID run,int page,int skip,Instant rs,Instant re,int status,int count,long bytes,boolean ok,String err){jdbc.update("INSERT INTO lta.collection_page_log(run_uid,page_no,skip_value,request_start_time,request_end_time,http_status,record_count,response_bytes,success,error_message) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(run_uid,page_no) DO UPDATE SET request_end_time=EXCLUDED.request_end_time,http_status=EXCLUDED.http_status,record_count=EXCLUDED.record_count,response_bytes=EXCLUDED.response_bytes,success=EXCLUDED.success,error_message=EXCLUDED.error_message",run,page,skip,ts(rs),ts(re),status,count,bytes,ok,err);}
 public void artifact(UUID run,CiqRawJsonArtifact a){jdbc.update("INSERT INTO lta.collection_artifact(run_uid,artifact_type,file_name,file_path,file_size_bytes,sha256,compression_type) VALUES(?,'RAW_JSON',?,?,?,?,'NONE')",run,a.path().getFileName().toString(),a.path().toString(),a.sizeBytes(),a.sha256());}
 public void dataArtifact(UUID run,String name,String path,long size,String sha){jdbc.update("INSERT INTO lta.collection_artifact(run_uid,artifact_type,file_name,file_path,file_size_bytes,sha256,compression_type) VALUES(?,'DATA_FILE',?,?,?,?,'NONE')",run,name,path,size,sha);}
 public void finish(UUID run,Instant end,Integer http,int pages,long records,long bytes,boolean ok,boolean complete,short retries,String err){jdbc.update("UPDATE lta.collection_run SET request_end_time=?,http_status=?,page_count=?,record_count=?,response_bytes=?,success=?,snapshot_complete=?,retry_count=?,consistency_status=?,error_message=?,update_time=CURRENT_TIMESTAMP WHERE uid=?",ts(end),http,pages,records,bytes,ok,complete,retries,ok?"COMPLETE":"FAILED",err,run);}
 private static Timestamp ts(Instant x){return x==null?null:Timestamp.from(x);}
}

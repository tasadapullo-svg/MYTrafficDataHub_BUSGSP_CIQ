package com.mytransitgps.modules.ciq.dashboard.service;

import com.mytransitgps.modules.ciq.dashboard.dto.CiqDashboardDtos;
import com.mytransitgps.modules.ciq.dashboard.repository.CiqDashboardRepository;
import com.mytransitgps.modules.ciq.dashboard.repository.CiqDashboardRepository.ApiRow;
import com.mytransitgps.modules.ciq.dashboard.repository.CiqDashboardRepository.ApiDataRow;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CIQ 大屏只读组合服务；不调用任何 Collector 或外部接口。 */
@Service
@ConditionalOnProperty(prefix="mytransitgps.database",name="enabled",havingValue="true")
@Transactional(readOnly = true, timeout = 30)
public class CiqDashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kuala_Lumpur");
    private static final Map<String,String> ZH = Map.of(
            "API01","道路分段实时速度等级","API02","高速公路分段预计旅行时间","API03","实时交通事件","API04","电子信息板实时文本",
            "API05","故障交通信号灯","API06","已批准道路施工","API07","历史小时平均交通流量","API08","计划道路开放");
    private static final Map<String,String> SCHEDULE = Map.of(
            "API01","15 min · 00/15/30/45","API02","15 min","API03","5 min","API04","5 min",
            "API05","1 hour","API06","24 hours","API07","Last Day of Every Month","API08","24 hours");
    private final CiqDashboardRepository repository;
    private final Path workspaceRoot;

    public CiqDashboardService(CiqDashboardRepository repository,
            @Value("${mytransitgps.workspace-root:data_download}") String workspaceRoot) {
        this.repository=repository; this.workspaceRoot=Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    public CiqDashboardDtos.Overview overview() {
        long executed=repository.countTodayRuns(null), successful=repository.countTodayRuns(true), failed=repository.countTodayRuns(false);
        LocalDate now=LocalDate.now(ZONE); long planned=794+(now.equals(YearMonth.from(now).atEndOfMonth())?1:0);
        return new CiqDashboardDtos.Overview(planned,executed,successful,failed,executed==0?null:successful*100.0/executed,Instant.now());
    }

    public List<CiqDashboardDtos.ApiStatus> apis() {
        Map<String, ApiDataRow> data = new LinkedHashMap<>();
        repository.apiDataRows().forEach(row -> data.put(row.code(), row));
        return repository.apiRows().stream().map(row -> toStatus(row, data.get(row.code()))).toList();
    }

    public CiqDashboardDtos.ApiStatus api(String code) {
        String normalized=code==null?"":code.toUpperCase();
        return apis().stream().filter(a->a.apiCode().equals(normalized)).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Unknown CIQ API code: "+code));
    }

    public List<CiqDashboardDtos.TrendPoint> trend(int hours) {
        int safe=Math.max(3,Math.min(hours,24*30));
        return repository.trend(safe).stream().map(m->new CiqDashboardDtos.TrendPoint((String)m.get("api_code"),
                ((java.sql.Timestamp)m.get("bucket_time")).toInstant(),((Number)m.get("executions")).longValue(),
                ((Number)m.get("records")).longValue())).toList();
    }

    public List<CiqDashboardDtos.AuditRow> audit() {
        List<CiqDashboardDtos.ApiStatus> apis=apis(); List<CiqDashboardDtos.AuditRow> rows=new ArrayList<>();
        rows.add(auditRow("01","Timeliness","时效性",apis,a->timeliness(a)));
        rows.add(auditRow("02","Completeness","完整性",apis,a->a.lastRequest()==null?cell("N/A","NA"):cell(a.completeness(),"COMPLETE".equals(a.completeness())?"SUCCESS":"WARNING")));
        rows.add(auditRow("03","Field Integrity","字段完整性",apis,a->cell("N/A","NA")));
        rows.add(auditRow("04","Duplicate Detection","重复数据",apis,a->cell("N/A","NA")));
        rows.add(auditRow("05","Null-value Check","空值检查",apis,a->cell("N/A","NA")));
        rows.add(auditRow("06","Timestamp Consistency","时间戳一致性",apis,a->cell(a.lastRequest()==null?"N/A":"AVAILABLE",a.lastRequest()==null?"NA":"SUCCESS")));
        rows.add(auditRow("07","Schema Match","模式匹配",apis,a->cell("N/A","NA")));
        rows.add(auditRow("08","Archive Status","归档状态",apis,a->archiveCell(a)));
        return rows;
    }

    public List<CiqDashboardDtos.RequestLog> requests(int limit) {
        return repository.recentRequests(Math.max(1,Math.min(limit,50))).stream().map(r->new CiqDashboardDtos.RequestLog(
                r.start(),r.code(),r.name(),r.http(),r.raw(),null,r.inserted(),duration(r.start(),r.end()),r.retry(),
                r.hashedArtifacts()>0?"HASHED":"--",r.success()?"SUCCESS":"ERROR")).toList();
    }

    public List<CiqDashboardDtos.MapPoint> mapPoints(String apiCode,int limit) {
        String code=apiCode==null?"ALL":apiCode.toUpperCase();
        if(!List.of("ALL","API01","API02").contains(code)) throw new IllegalArgumentException("Map supports API01, API02 or ALL");
        return repository.mapPoints(code,limit).stream().map(p->new CiqDashboardDtos.MapPoint(p.apiCode(),p.recordId(),p.title(),p.category(),p.latitude(),p.longitude(),p.observedAt(),p.details())).toList();
    }

    public List<CiqDashboardDtos.DataRecord> dataRecords(String apiCode,Instant from,Instant to,String category,int limit) {
        String code=apiCode==null?"ALL":apiCode.toUpperCase();
        if(!List.of("ALL","API03","API04","API05","API06","API07","API08").contains(code)) throw new IllegalArgumentException("Data query supports API03-API08 or ALL");
        if(from!=null&&to!=null&&from.isAfter(to)) throw new IllegalArgumentException("from must be before to");
        return repository.dataRecords(code,from,to,category,limit).stream().map(r->new CiqDashboardDtos.DataRecord(r.apiCode(),r.recordId(),r.category(),r.title(),r.description(),r.eventTime(),r.status(),r.latitude(),r.longitude(),r.details())).toList();
    }

    public CiqDashboardDtos.Storage storage() {
        var s=repository.storage(); Long total=null,free=null,used=null; Double pct=null;
        try { FileStore fs=Files.getFileStore(workspaceRoot); total=fs.getTotalSpace();free=fs.getUsableSpace();used=total-free;pct=total==0?null:used*100.0/total; } catch(Exception ignored) { }
        return new CiqDashboardDtos.Storage(s.todayFiles(),s.totalFiles(),s.todayBytes(),s.totalBytes(),s.archiveCount(),s.latestArchive(),
                s.archiveCount()>0?"AVAILABLE":"--",s.dbInserted(),s.dbTotal(),s.latestDb(),"AVAILABLE",total,used,free,pct,Instant.now());
    }

    public CiqDashboardDtos.DashboardPayload payload(int trendHours) { return new CiqDashboardDtos.DashboardPayload(overview(),apis(),trend(trendHours),audit(),requests(20),storage()); }

    private CiqDashboardDtos.ApiStatus toStatus(ApiRow r, ApiDataRow data) {
        String status=r.runUid()==null?("API07".equals(r.code())?"WAITING":"NOT_AVAILABLE"):Boolean.TRUE.equals(r.success())?"SUCCESS":"ERROR";
        Map<String,Object> metrics=new LinkedHashMap<>();
        if ("API07".equals(r.code())) {
            metrics.put("currentMonth",YearMonth.now(ZONE).toString());
            metrics.put("downloadStatus",r.runUid()==null?"WAITING":status);
            metrics.put("parseStatus",r.runUid()==null?null:r.complete()?"PARSED":"--");
            metrics.put("databaseStatus",r.runUid()==null?null:r.inserted()!=null?"PROCESSED":"--");
        }
        Instant latestDataTime=data==null?null:data.latestDataTime();
        Long todayCount=data==null?null:data.todayCount();
        Long totalCount=data==null?null:data.totalCount();
        return new CiqDashboardDtos.ApiStatus(r.code(),r.name(),ZH.get(r.code()),r.endpoint(),SCHEDULE.get(r.code()),status,
                r.start(),r.lastSuccess(),r.lastSuccess(),latestDataTime,todayCount,totalCount,r.http(),r.raw(),r.selected(),r.inserted(),r.todayExecutions(),
                r.runUid()==null?"N/A":Boolean.TRUE.equals(r.complete())?"COMPLETE":"INCOMPLETE",r.retries(),r.pages(),r.bytes(),
                duration(r.start(),r.end()),r.error(),r.fileName(),r.fileSize(),r.sha(),metrics);
    }

    private CiqDashboardDtos.AuditCell timeliness(CiqDashboardDtos.ApiStatus a){
        if(a.lastRequest()==null)return cell("API07".equals(a.apiCode())?"WAITING":"N/A","API07".equals(a.apiCode())?"WAITING":"NA");
        long age=Duration.between(a.lastRequest(),Instant.now()).toMinutes(); long limit=switch(a.apiCode()){case "API03","API04"->15;case "API05"->120;case "API06","API08"->2880;case "API07"->44640;default->45;};
        return cell(age<=limit?"ON TIME":"DELAYED",age<=limit?"SUCCESS":"WARNING");
    }
    private CiqDashboardDtos.AuditCell archiveCell(CiqDashboardDtos.ApiStatus a){if(a.lastRequest()==null)return cell("API07".equals(a.apiCode())?"WAITING":"N/A","API07".equals(a.apiCode())?"WAITING":"NA");boolean ok="SUCCESS".equals(a.status())&&a.sha256()!=null&&"COMPLETE".equals(a.completeness());return cell(ok?"ARCHIVED":"INCOMPLETE",ok?"SUCCESS":"WARNING");}
    private CiqDashboardDtos.AuditRow auditRow(String code,String en,String zh,List<CiqDashboardDtos.ApiStatus> apis,java.util.function.Function<CiqDashboardDtos.ApiStatus,CiqDashboardDtos.AuditCell> fn){Map<String,CiqDashboardDtos.AuditCell> m=new LinkedHashMap<>();apis.forEach(a->m.put(a.apiCode(),fn.apply(a)));return new CiqDashboardDtos.AuditRow(code,en,zh,m);}
    private static CiqDashboardDtos.AuditCell cell(String t,String s){return new CiqDashboardDtos.AuditCell(t,s);}
    private static Long duration(Instant a,Instant b){return a==null||b==null?null:Math.max(0,Duration.between(a,b).toMillis());}
}

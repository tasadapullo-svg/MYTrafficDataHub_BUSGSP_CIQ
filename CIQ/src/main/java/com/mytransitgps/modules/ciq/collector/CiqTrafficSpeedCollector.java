package com.mytransitgps.modules.ciq.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytransitgps.modules.ciq.client.LtaHttpResponse;
import com.mytransitgps.modules.ciq.client.LtaTrafficSpeedClient;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.modules.ciq.domain.TrafficSpeedBandRecord;
import com.mytransitgps.modules.ciq.parser.TrafficSpeedBandsParser;
import com.mytransitgps.modules.ciq.persistence.CiqTrafficSpeedPersistenceService;
import com.mytransitgps.modules.ciq.persistence.TrafficSpeedPageData;
import com.mytransitgps.modules.ciq.persistence.TrafficSpeedPersistenceResult;
import com.mytransitgps.modules.ciq.quality.CiqTrafficSpeedValidator;
import com.mytransitgps.modules.ciq.quality.TrafficSpeedQualityStatus;
import com.mytransitgps.modules.ciq.storage.CiqApiCode;
import com.mytransitgps.modules.ciq.storage.CiqJsonStorageService;
import com.mytransitgps.modules.ciq.storage.CiqRawJsonArtifact;
import com.mytransitgps.platform.collection.*;
import com.mytransitgps.platform.http.HttpRequestResult;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CIQ API01 TrafficSpeedBands 采集器。
 * Raw 全岛快照是第一优先级：即使 PostGIS/study_area/数据库暂时不可用，也继续完整分页并落盘；
 * 数据库异常只把本轮降级为 PARTIAL_SUCCESS，不再阻断 Raw 科研证据采集。
 */
public class CiqTrafficSpeedCollector implements DataCollector {
    private static final Logger log = LoggerFactory.getLogger(CiqTrafficSpeedCollector.class);
    private final LtaTrafficSpeedClient client;
    private final CiqProperties properties;
    private final CiqJsonStorageService storageService;
    private final ObjectMapper objectMapper;
    private final TrafficSpeedBandsParser parser;
    private final CiqTrafficSpeedValidator validator;
    private final CiqTrafficSpeedPersistenceService persistenceService;

    public CiqTrafficSpeedCollector(LtaTrafficSpeedClient c, CiqProperties p, CiqJsonStorageService s, ObjectMapper m) {
        this(c,p,s,m,new TrafficSpeedBandsParser(m),new CiqTrafficSpeedValidator(),null);
    }
    public CiqTrafficSpeedCollector(LtaTrafficSpeedClient c, CiqProperties p, CiqJsonStorageService s, ObjectMapper m,
                                    TrafficSpeedBandsParser parser, CiqTrafficSpeedValidator validator,
                                    CiqTrafficSpeedPersistenceService persistenceService) {
        this.client=c;this.properties=p;this.storageService=s;this.objectMapper=m;this.parser=parser;this.validator=validator;this.persistenceService=persistenceService;
    }
    public ModuleCode moduleCode(){return ModuleCode.CIQ;}
    public CollectorCode collectorCode(){return CollectorCode.CIQ_TRAFFIC_SPEED;}

    public CollectionResult collect(CollectionContext context) {
        Instant start=Instant.now();
        log.info("CIQ API01 [N01] Collector进入，runUid={}，scheduledTime={}，manual={}，dbWriteRequested={}，accountKeyConfigured={}",
                context.runUid(),context.scheduledTime(),context.manualTrigger(),properties.getPersistence().isDatabaseWriteEnabled(),properties.isAccountKeyConfigured());
        if(!properties.isAccountKeyConfigured()) return CollectionResult.skipped(context,CollectionStatus.NOT_CONFIGURED,start,"LTA_ACCOUNT_KEY_MISSING","LTA AccountKey is not configured");

        boolean dbRequested=properties.getPersistence().isDatabaseWriteEnabled();
        boolean dbActive=false;
        String dbErrorCode=null, dbErrorMessage=null;
        if(dbRequested){
            log.info("CIQ API01 [N02] 检查Persistence Bean和6个study_area，runUid={}",context.runUid());
            if(persistenceService==null){dbErrorCode="DATABASE_PERSISTENCE_NOT_AVAILABLE";dbErrorMessage="CiqTrafficSpeedPersistenceService Bean未装配";log.error("CIQ API01 [DB-N00] {}",dbErrorMessage);}
            else try{
                if(!persistenceService.hasRequiredStudyAreas()){dbErrorCode="STUDY_AREA_NOT_READY";dbErrorMessage="WOODLANDS/TUAS CORE/APPROACH/CORRIDOR study_area未就绪";log.error("CIQ API01 [DB-N00] {}",dbErrorMessage);}
                else{
                    log.info("CIQ API01 [DB-N01] collection_run初始化，runUid={}",context.runUid());
                    persistenceService.beginRun(context.runUid(),persistenceService.apiEndpointUid("API01"),context.manualTrigger()?"MANUAL_TEST":"SCHEDULED",context.scheduledTime(),start);
                    dbActive=true;
                }
            }catch(RuntimeException ex){dbErrorCode="DATABASE_AUDIT_INIT_FAILED";dbErrorMessage=ex.getMessage();log.error("CIQ API01 [DB-N00] 数据库初始化失败但继续Raw采集，runUid={}，message={}",context.runUid(),ex.getMessage(),ex);}
        } else {
            log.info("CIQ API01 [N02] 数据库写入关闭，本轮执行Raw-only采集，runUid={}", context.runUid());
        }

        int requestCount=0; long received=0,valid=0,rejected=0,dup=0,rawBytes=0; short retries=0; Integer httpStatus=null;
        int maxPages=Math.max(1,properties.getCollectors().getTrafficSpeed().getMaxPages());
        int pageSize=Math.max(1,properties.getCollectors().getTrafficSpeed().getPageSize());
        Set<String> visited=new HashSet<>(); URI uri=client.firstPageUri(); Instant snapshot=start;
        TrafficSpeedPersistenceResult totals=TrafficSpeedPersistenceResult.empty();

        for(int pageNo=1;pageNo<=maxPages&&uri!=null;pageNo++){
            if(!visited.add(uri.toString())) return rawFailure(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,"PAGINATION_LOOP_DETECTED","Repeated pagination URI detected");
            log.info("CIQ API01 [N03] HTTP页面请求开始，runUid={}，page={}，skip={}，endpoint={}",context.runUid(),pageNo,(pageNo-1)*pageSize,uri);
            LtaHttpResponse rr=client.fetchPage(uri); HttpRequestResult http=rr.http(); requestCount++; retries+=rr.retryCount(); httpStatus=http.statusCode();
            if(!http.successful()){
                log.error("CIQ API01 [N03-ERR] HTTP最终失败，runUid={}，page={}，status={}，errorCode={}，message={}",context.runUid(),pageNo,http.statusCode(),http.errorCode(),http.errorMessage());
                safeFinalize(context,requestCount,received,rawBytes,false,false,retries,http.errorMessage(),httpStatus,dbActive);
                return rawFailure(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,http.errorCode()==null?"HTTP_REQUEST_FAILED":http.errorCode(),http.errorMessage());
            }
            log.info("CIQ API01 [N04] HTTP成功，runUid={}，page={}，status={}，bytes={}，latencyMs={}",context.runUid(),pageNo,http.statusCode(),http.body().length,http.latencyMs());
            LocalDate date=http.requestEndTime().atZone(ZoneId.of(properties.getTimezone())).toLocalDate();
            CiqRawJsonArtifact art;
            try{art=storageService.saveRawJson(CiqApiCode.API01,date,context.runUid(),pageNo,http.requestEndTime(),http.body());rawBytes+=http.body().length;}
            catch(RuntimeException ex){safeFinalize(context,requestCount,received,rawBytes,false,false,retries,ex.getMessage(),httpStatus,dbActive);return rawFailure(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,"RAW_SAVE_FAILED",ex.getMessage());}
            log.info("CIQ API01 [N05] Raw落盘完成，runUid={}，page={}，path={}，sha256={}",context.runUid(),pageNo,art.path(),art.sha256());

            if(dbActive){
                try{
                    persistenceService.registerArtifact(context.runUid(),art);
                }catch(RuntimeException ex){
                    dbErrorCode="DATABASE_ARTIFACT_REGISTER_FAILED";dbErrorMessage=ex.getMessage();
                    log.error("CIQ API01 [DB-N98] Raw artifact登记失败，本轮后续降级为Raw采集，runUid={}，page={}，message={}",context.runUid(),pageNo,ex.getMessage(),ex);
                    safeFinalize(context,requestCount,received,rawBytes,false,false,retries,ex.getMessage(),httpStatus,true); dbActive=false;
                }
            }
            JsonNode root; List<TrafficSpeedBandRecord> parsed;
            try{root=objectMapper.readTree(http.body());parsed=parser.parse(http.body());}
            catch(Exception ex){safeFinalize(context,requestCount,received,rawBytes,false,false,retries,ex.getMessage(),httpStatus,dbActive);return rawFailure(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,"INVALID_JSON",ex.getMessage());}
            int records=recordCount(root); received+=records;
            var qc=validator.validatePage(parsed); long pv=qc.stream().filter(x->x.status()==TrafficSpeedQualityStatus.VALID).count(); long pw=qc.stream().filter(x->x.status()==TrafficSpeedQualityStatus.WARNING).count(); long pr=qc.stream().filter(x->x.status()==TrafficSpeedQualityStatus.REJECTED).count(); long pd=qc.stream().filter(x->x.duplicateInPage()).count();valid+=pv+pw;rejected+=pr;dup+=pd;
            log.info("CIQ API01 [N06] Parser/Validator完成，runUid={}，page={}，records={}，parsed={}，valid={}，warning={}，rejected={}，duplicates={}",context.runUid(),pageNo,records,parsed.size(),pv,pw,pr,pd);

            if(dbActive){
                try{
                    persistenceService.registerArtifact(context.runUid(),art);
                    persistenceService.logPage(context.runUid(),pageNo,(pageNo-1)*pageSize,http.requestStartTime(),http.requestEndTime(),http.statusCode(),records,http.body().length,true,null);
                    log.info("CIQ API01 [N07] 开始PostGIS筛选和DB写入，runUid={}，page={}，accepted={}",context.runUid(),pageNo,pv+pw);
                    var r=persistenceService.persistPage(context.runUid(),snapshot,new TrafficSpeedPageData(pageNo,(pageNo-1)*pageSize,parsed,qc));totals=totals.plus(r);
                    log.info("CIQ API01 [DB-N02] 页面入库完成，runUid={}，page={}，inScope={}，inserted={}，duplicates={}，outOfScope={}",context.runUid(),pageNo,r.uniqueInScopeLinks(),r.observationsInserted(),r.observationDuplicates(),r.outOfScopeCount());
                }catch(RuntimeException ex){
                    dbErrorCode="DATABASE_WRITE_FAILED";dbErrorMessage=ex.getMessage();
                    log.error("CIQ API01 [DB-N99] 数据库节点失败，本轮后续继续完整Raw采集，runUid={}，page={}，message={}",context.runUid(),pageNo,ex.getMessage(),ex);
                    safeFinalize(context,requestCount,received,rawBytes,false,false,retries,ex.getMessage(),httpStatus,true); dbActive=false;
                }
            }
            log.info("CIQ API01 [N08] 页面闭环完成，runUid={}，page={}，records={}，receivedTotal={}，rawBytesTotal={}",context.runUid(),pageNo,records,received,rawBytes);
            URI next=nextUri(root,pageNo,records,pageSize);
            if(next==null){
                if(dbActive){safeFinalize(context,requestCount,received,rawBytes,true,true,retries,null,httpStatus,true);log.info("CIQ API01 [N09] 完整Raw + PostGIS + PostgreSQL闭环完成，runUid={}，pages={}，rawRecords={}，inScope={}，inserted={}，outOfScope={}",context.runUid(),requestCount,received,totals.uniqueInScopeLinks(),totals.observationsInserted(),totals.outOfScopeCount());return success(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus);}
                if (dbRequested) {
                    log.warn("CIQ API01 [N09] Raw完整快照已完成，但数据库链未完成，runUid={}，pages={}，rawRecords={}，dbErrorCode={}，dbErrorMessage={}",context.runUid(),requestCount,received,dbErrorCode,dbErrorMessage);
                    return rawCompletePartial(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,true,dbErrorCode,dbErrorMessage);
                }
                log.info("CIQ API01 [N09] Raw-only完整快照完成，runUid={}，pages={}，rawRecords={}，valid={}，rejected={}", context.runUid(), requestCount, received, valid, rejected);
                return rawOnlySuccess(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus);
            }
            uri=next;
        }
        safeFinalize(context,requestCount,received,rawBytes,false,false,retries,"MAX_PAGES_REACHED",httpStatus,dbActive);
        return rawFailure(context,start,requestCount,received,valid,rejected,dup,totals,httpStatus,"MAX_PAGES_REACHED","API01 reached max-pages before pagination ended");
    }

    private void safeFinalize(CollectionContext c,int pages,long records,long bytes,boolean ok,boolean complete,short retries,String err,Integer http,boolean active){if(!active||persistenceService==null)return;try{persistenceService.finalizeRun(c.runUid(),Instant.now(),http==null?0:http,pages,records,bytes,ok,complete,retries,err);}catch(Exception ex){log.error("CIQ API01 [DB-N98] collection_run finalize失败，runUid={}，message={}",c.runUid(),ex.getMessage(),ex);}}
    private CollectionResult success(CollectionContext c,Instant s,int req,long rec,long valid,long rejected,long dup,TrafficSpeedPersistenceResult t,Integer http){Instant e=Instant.now();return new CollectionResult(c.runUid(),ModuleCode.CIQ,collectorCode(),true,CollectionStatus.SUCCESS,s,e,Duration.between(s,e).toMillis(),req,rec,valid,dup+t.observationDuplicates(),t.observationsInserted(),t.newLinkCount()+t.updatedLinkCount(),rejected,0,http,null,null);}
    private CollectionResult rawOnlySuccess(CollectionContext c,Instant s,int req,long rec,long valid,long rejected,long dup,TrafficSpeedPersistenceResult t,Integer http){Instant e=Instant.now();return new CollectionResult(c.runUid(),ModuleCode.CIQ,collectorCode(),true,CollectionStatus.SUCCESS,s,e,Duration.between(s,e).toMillis(),req,rec,valid,dup+t.observationDuplicates(),0,0,rejected,0,http,null,null);}
    private CollectionResult rawCompletePartial(CollectionContext c,Instant s,int req,long rec,long valid,long rejected,long dup,TrafficSpeedPersistenceResult t,Integer http,boolean dbRequested,String ec,String em){Instant e=Instant.now();String code=dbRequested?(ec==null?"DATABASE_WRITE_SKIPPED_RAW_SAVED":ec):"DATABASE_WRITE_DISABLED_RAW_SAVED";String msg=dbRequested?(em==null?"Raw snapshot complete; database chain unavailable":em):"Raw snapshot complete; database write disabled";return new CollectionResult(c.runUid(),ModuleCode.CIQ,collectorCode(),false,CollectionStatus.PARTIAL_SUCCESS,s,e,Duration.between(s,e).toMillis(),req,rec,valid,dup+t.observationDuplicates(),t.observationsInserted(),t.newLinkCount()+t.updatedLinkCount(),rejected,0,http,code,msg);}
    private CollectionResult rawFailure(CollectionContext c,Instant s,int req,long rec,long valid,long rejected,long dup,TrafficSpeedPersistenceResult t,Integer http,String ec,String em){Instant e=Instant.now();return new CollectionResult(c.runUid(),ModuleCode.CIQ,collectorCode(),false,rec>0?CollectionStatus.PARTIAL_SUCCESS:CollectionStatus.FAILED,s,e,Duration.between(s,e).toMillis(),req,rec,valid,dup+t.observationDuplicates(),t.observationsInserted(),t.newLinkCount()+t.updatedLinkCount(),rejected,1,http,ec,em);}
    private static int recordCount(JsonNode root){if(root==null)return 0;if(root.isArray())return root.size();JsonNode v=root.get("value");return v!=null&&v.isArray()?v.size():0;}
    private URI nextUri(JsonNode root,int pageNo,int records,int pageSize){String next=text(root,"@odata.nextLink");if(next==null)next=text(root,"odata.nextLink");if(next==null)next=text(root,"nextLink");if(next!=null&&!next.isBlank())return URI.create(next);if(records<pageSize)return null;URI base=client.firstPageUri();return URI.create(base.toString()+(base.toString().contains("?")?"&":"?")+"$skip="+((long)pageNo*pageSize));}
    private static String text(JsonNode root,String field){if(root==null||!root.isObject())return null;JsonNode n=root.get(field);return n==null||n.isNull()?null:n.asText(null);}
}


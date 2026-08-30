package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.HttpRequestResult;
import com.mytransitgps.platform.http.TrafficHttpClient;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
/** API07 Client：先获取 Link，再无认证下载临时数据文件；外部文件只允许 HTTPS，绝不携带 AccountKey。 */
public class LtaTrafficFlowClient extends AbstractLtaJsonClient {
 private static final Logger log=LoggerFactory.getLogger(LtaTrafficFlowClient.class);
 public LtaTrafficFlowClient(TrafficHttpClient h,CiqProperties p){super(h,p,"API07",p.getCollectors().getTrafficFlow());}
 public HttpRequestResult downloadDataFile(URI uri){
   if(uri==null||!uri.isAbsolute()||!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("API07数据文件URL必须为HTTPS绝对地址");
   URI current=uri;
   for(int redirect=0;redirect<=5;redirect++){
     log.info("CIQ API07 [FILE-N01] 下载原始文件，uri={}，redirect={}",current,redirect);
     HttpRequestResult r=httpClient.get(current,Map.of(),Duration.ofSeconds(properties.getHttp().getConnectTimeoutSeconds()),Duration.ofSeconds(Math.max(60,properties.getHttp().getRequestTimeoutSeconds())));
     if(r.statusCode()>=300&&r.statusCode()<400){
       String loc=r.headers().entrySet().stream().filter(e->e.getKey().equalsIgnoreCase("Location")).flatMap(e->e.getValue().stream()).findFirst().orElse(null);
       if(loc==null) return r; URI next=current.resolve(loc);
       if(!"https".equalsIgnoreCase(next.getScheme())) throw new IllegalArgumentException("API07重定向目标必须为HTTPS");
       current=next; continue;
     }
     log.info("CIQ API07 [FILE-N02] 文件响应完成，status={}，bytes={}，latencyMs={}",r.statusCode(),r.body().length,r.latencyMs()); return r;
   }
   throw new IllegalStateException("API07数据文件重定向超过5次");
 }
}

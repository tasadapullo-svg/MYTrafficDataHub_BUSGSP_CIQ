package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API08 独立 LTA Client。 */
public class LtaRoadOpeningsClient extends AbstractLtaJsonClient {
 public LtaRoadOpeningsClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API08",properties.getCollectors().getRoadOpenings());}
}

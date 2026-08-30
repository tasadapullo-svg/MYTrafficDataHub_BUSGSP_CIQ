package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API06 独立 LTA Client。 */
public class LtaRoadWorksClient extends AbstractLtaJsonClient {
 public LtaRoadWorksClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API06",properties.getCollectors().getRoadWorks());}
}

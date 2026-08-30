package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API03 独立 LTA Client。 */
public class LtaTrafficIncidentsClient extends AbstractLtaJsonClient {
 public LtaTrafficIncidentsClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API03",properties.getCollectors().getTrafficIncidents());}
}

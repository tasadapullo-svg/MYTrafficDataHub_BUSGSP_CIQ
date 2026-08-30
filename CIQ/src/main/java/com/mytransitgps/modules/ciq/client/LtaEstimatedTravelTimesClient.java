package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API02 独立 LTA Client。 */
public class LtaEstimatedTravelTimesClient extends AbstractLtaJsonClient {
 public LtaEstimatedTravelTimesClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API02",properties.getCollectors().getEstimatedTravelTimes());}
}

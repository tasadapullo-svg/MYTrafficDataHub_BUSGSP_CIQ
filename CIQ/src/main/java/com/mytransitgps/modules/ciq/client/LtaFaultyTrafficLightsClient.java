package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API05 独立 LTA Client。 */
public class LtaFaultyTrafficLightsClient extends AbstractLtaJsonClient {
 public LtaFaultyTrafficLightsClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API05",properties.getCollectors().getFaultyTrafficLights());}
}

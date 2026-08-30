package com.mytransitgps.modules.ciq.client;
import com.mytransitgps.modules.ciq.config.CiqProperties;
import com.mytransitgps.platform.http.TrafficHttpClient;
/** API04 独立 LTA Client。 */
public class LtaVmsClient extends AbstractLtaJsonClient {
 public LtaVmsClient(TrafficHttpClient httpClient,CiqProperties properties){super(httpClient,properties,"API04",properties.getCollectors().getVms());}
}

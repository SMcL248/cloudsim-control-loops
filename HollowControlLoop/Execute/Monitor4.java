package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
 
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor4 implements Monitor<Map<HostEntity, Map<String, Double>>>{

    @Override
    public Map<HostEntity, Map<String, Double>> observe(ReadSpace readSpace) {

        double now = readSpace.getNow();

        Log.printlnConcat(now, ": Observing...");
        

            Map<HostEntity, Map<String, Double>> metrics = new HashMap<>();

            for (HostEntity host : readSpace.getAllHosts()){

                double totalBw = host.getBw();
                double usedBw = totalBw - host.getGuestBwProvisioner().getAvailableBw();
                double util = usedBw/totalBw;

                Log.printlnConcat("Host #", host.getId(), "| Utilised bandwidth ", util);
                metrics.put(host, Map.of("bw_util", util));

            }

        return metrics;

    }

    @Override
    public String outputGuid() {

        return "bw_util";

    }
    
}

package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
 
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor4 implements Monitor<Map<Integer, Map<HostEntity, Map<String, Double>>>>{

    @Override
    public Map<Integer, Map<HostEntity, Map<String, Double>>> observe(WorldState worldState) {

        double now = worldState.now();

        Log.printlnConcat(now, ": Observing...");

        Map<Integer, Map<HostEntity, Map<String, Double>>> metrics = new HashMap<>();
        
        for (Integer id: worldState.hosts().keySet()){

            Map<HostEntity, Map<String, Double>> first_map = new HashMap<>();

            for (HostEntity host : worldState.hosts().get(id)){

                double totalBw = host.getBw();
                double usedBw = totalBw - host.getGuestBwProvisioner().getAvailableBw();
                double util = usedBw/totalBw;

                Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), "| Utilised bandwidth ", util);
                first_map.put(host, Map.of("bw_util", util));

            }

            metrics.put(id, first_map);

        }

        return metrics;

    }

    @Override
    public Set<String> providedMetrics() {

        return Set.of("bw_util");

    }
    
}

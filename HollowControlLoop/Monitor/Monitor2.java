package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor2 implements Monitor<Map<Integer, Map<HostEntity, Map<String, Double>>>>{

    @Override
    public Map<Integer, Map<HostEntity, Map<String, Double>>> observe(WorldState worldState) {

        double now = worldState.now();

        Log.printlnConcat(now, ": Observing...");

        Map<Integer, Map<HostEntity, Map<String, Double>>> metrics = new HashMap<>();
        
        for (Integer id: worldState.hosts().keySet()){

            Map<HostEntity, Map<String, Double>> first_map = new HashMap<>();

            for (HostEntity host : worldState.hosts().get(id)){

                double total = host.getTotalMips();

                double used = 0;

                for (GuestEntity vm : host.getGuestList()){
                    used += vm.getTotalUtilizationOfCpuMips(now); 
                }

                double util = used/total;
                Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), "| Total CPU Utilisation ", util);
                first_map.put(host, Map.of("cpu_util", util));

            }

            metrics.put(id, first_map);

        }

        return metrics;

    }

    @Override
    public Set<String> providedMetrics() {

        return Set.of("cpu_util");

    }
    
}

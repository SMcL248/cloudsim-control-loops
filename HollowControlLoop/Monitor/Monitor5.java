package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor5 implements Monitor<Map<Integer, Map<HostEntity, Map<String, Double>>>>{

    @Override
    public Map<Integer, Map<HostEntity, Map<String, Double>>> observe(WorldState worldState) {

        double now = worldState.now();
    
        Log.printlnConcat(now, ": Observing...");

        Map<Integer, Map<HostEntity, Map<String, Double>>> metrics = new HashMap<>();
        
        for (Integer id: worldState.hosts().keySet()){

            Map<HostEntity, Map<String, Double>> firstMap = new HashMap<>();

            for (HostEntity host : worldState.hosts().get(id)){

                Map<String, Double> hostMetrics = new HashMap<>();

                double totalCpu = host.getTotalMips();

                double usedCpu = 0;

                for (GuestEntity vm : host.getGuestList()){
                    usedCpu += vm.getTotalUtilizationOfCpuMips(now); 
                }

                double utilCpu = usedCpu/totalCpu;

                double totalRam = host.getRam();
                double usedRam = totalRam - host.getGuestRamProvisioner().getAvailableRam();
                double utilRam = usedRam/totalRam;

                double totalBw = host.getBw();
                double usedBw = totalBw - host.getGuestBwProvisioner().getAvailableBw();
                double utilBw = usedBw/totalBw;
                
                hostMetrics.put("cpu_util",utilCpu);
                hostMetrics.put("ram_util", utilRam);
                hostMetrics.put("bw_util", utilBw);

                Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), "| Utilised CPU ", utilCpu, "| Utilised RAM ", utilRam, "| Utilised bandwidth ", utilBw);
                firstMap.put(host, hostMetrics);

            }

            metrics.put(id, firstMap);

        }

        return metrics;

    }

    @Override
    public Set<String> providedMetrics() {

        return Set.of("cpu_util", "ram_util", "bw_util");

    }
    
}

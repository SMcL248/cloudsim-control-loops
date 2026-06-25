package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor5 implements Monitor<Map<HostEntity, Map<String, Double>>>{

    @Override
    public Map<HostEntity, Map<String, Double>> observe(ReadSpace readSpace) {

        double now = readSpace.getNow();
    
        Log.printlnConcat(now, ": Observing...");
        
            Map<HostEntity, Map<String, Double>> metrics = new HashMap<>();

            for (HostEntity host :readSpace.getAllHosts()){

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

                Log.printlnConcat("Host #", host.getId(), "| Utilised CPU ", utilCpu, "| Utilised RAM ", utilRam, "| Utilised bandwidth ", utilBw);
                metrics.put(host, hostMetrics);

            }

        return metrics;

    }

    @Override
    public String outputGuid() {

        return "cpu-ram-bw-util";

    }
    
}

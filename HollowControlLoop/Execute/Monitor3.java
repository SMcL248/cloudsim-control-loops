package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor3 implements Monitor<Map<HostEntity, Map<String, Double>>>{

    @Override
    public Map<HostEntity, Map<String, Double>> observe(ReadSpace readSpace) {

        double now = readSpace.getNow();

        Log.printlnConcat(now, ": Observing...");

        Map<HostEntity, Map<String, Double>> metrics = new HashMap<>();

        for (HostEntity host : readSpace.getAllHosts()){

            double totalRam = host.getRam();
            double usedRam = totalRam - host.getGuestRamProvisioner().getAvailableRam();
            double util = usedRam/totalRam;

            Log.printlnConcat("Host #", host.getId(), "| Utilised RAM ", util);
            metrics.put(host, Map.of("ram_util", util));

        }

        return metrics;

    }

    @Override
    public String outputGuid() {

        return "ram_util";

    }
    
}

package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public class Monitor2 implements Monitor<Map<HostEntity, Map<String, Double>>>{

    @Override
    public Map<HostEntity, Map<String, Double>> observe(ReadSpace readSpace) {

        double now = readSpace.getNow();

        Log.printlnConcat(now, ": Observing...");

        Map<HostEntity, Map<String, Double>> metrics = new HashMap<>();

        for (HostEntity host : readSpace.getAllHosts()){

            double total = host.getTotalMips();

            double used = 0;

            for (GuestEntity vm : host.getGuestList()){
                used += vm.getTotalUtilizationOfCpuMips(now); 
            }

            double util = used/total;

            Log.printlnConcat( "Host #", host.getId(), "| Total CPU Utilisation ", util);
            metrics.put(host, Map.of("cpu_util", util));

        }

        return metrics;

    }

    @Override
    public String outputGuid() {

        return "cpu_util";

    }

}
    


package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.lang.Math;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;   

public class Analyser6 implements Analyser<Map<Integer, Map<HostEntity, Map<String, Double>>>, Diagnosis<HostEntity>> {

    private static final double UPPER_THRESHOLD = 0.8;
    private static final double LOWER_THRESHOLD = 0.2;

    @Override
    public Diagnosis<HostEntity> analyse(Map<Integer, Map<HostEntity, Map<String, Double>>> metrics) {

        Map<HostEntity, LoadState> classification = new HashMap<>();
        Map<HostEntity, Integer> datacenterIds = new HashMap<>();
        Map<HostEntity, Double> costScores = new HashMap<>();

        //Iterate by datacenter
        for (var dcEntry: metrics.entrySet()) {

            int datacenterId = dcEntry.getKey();

            //Iterate by Host
            for (var hostEntry : metrics.get(datacenterId).entrySet()) {

                // Calculate cost function
                HostEntity host = hostEntry.getKey();
                Map<String, Double> m = hostEntry.getValue();
                double cost = Math.max(Math.max(m.get("cpu_util"), m.get("ram_util")), m.get("bw_util"));
     
                costScores.put(host, cost);
                datacenterIds.put(host, datacenterId);

            }
        }

        // Determind load
        for (var entry : costScores.entrySet()){

            HostEntity host = entry.getKey();
            double cost = entry.getValue();

            // Detemine load level
            if (cost > UPPER_THRESHOLD) {
                classification.put(host, LoadState.OVERLOADED);
                Log.printlnConcat( "Datacenter #", datacenterIds.get(host), "| Host #", host.getId(), " is overloaded.");
            } else if (cost < LOWER_THRESHOLD) {
                classification.put(host, LoadState.UNDERLOADED);
                Log.printlnConcat("Datacenter #", datacenterIds.get(host), "| Host #", host.getId(), " is underloaded.");
            } else {
                classification.put(host, LoadState.BALANCED);
                Log.printlnConcat("Datacenter #", datacenterIds.get(host), "| Host #", host.getId(), " is balanced.");
            }

        }

        return new Diagnosis<>(classification, costScores, datacenterIds);

    }

    @Override
    public Set<String> requiredMetrics() {
        return Set.of("cpu_util", "ram_util", "bw_util");
    }
}
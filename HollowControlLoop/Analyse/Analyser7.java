package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.lang.Math;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;   

public class Analyser7 implements Analyser< Map<HostEntity, Map<String, Double>>, Diagnosis<HostEntity>> {

    private static final double UPPER_THRESHOLD = 0.8;
    private static final double LOWER_THRESHOLD = 0.2;

    @Override
    public Diagnosis<HostEntity> analyse(Map<HostEntity, Map<String, Double>> metrics, ReadSpace readSpace) {  

        Map<HostEntity, LoadState> classification = new HashMap<>();
        Map<HostEntity, Double> costScores = new HashMap<>();

        //Iterate by Host
        for (var hostEntry : metrics.entrySet()) {

            // Calculate cost function
            HostEntity host = hostEntry.getKey();
            Map<String, Double> m = hostEntry.getValue();
            double cost = Math.max(Math.max(m.get("cpu_util"), m.get("ram_util")), m.get("bw_util"));
    
            costScores.put(host, cost);

        }
    

        // Determind load
        for (var entry : costScores.entrySet()){

            HostEntity host = entry.getKey();
            double cost = entry.getValue();

            // Detemine load level
            if (cost > UPPER_THRESHOLD) {
                classification.put(host, LoadState.OVERLOADED);
                Log.printlnConcat( "Host #", host.getId(), " is overloaded.");
            } else if (cost < LOWER_THRESHOLD) {
                classification.put(host, LoadState.UNDERLOADED);
                Log.printlnConcat("Host #", host.getId(), " is underloaded.");
            } else {
                classification.put(host, LoadState.BALANCED);
                Log.printlnConcat("Host #", host.getId(), " is balanced.");
            }

        }


        return new Diagnosis<>(classification, costScores);

    }

    @Override
    public String inputGuid() {
        return "host-cpu-ram-bw";
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }
    
}
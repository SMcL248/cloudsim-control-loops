package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;   

public class Analyser5 implements Analyser<Map<HostEntity, Map<String, Double>>, Diagnosis<HostEntity>> {

    // Cost function: cost(Host) = alpha*cpu_util + beta*ram_util + gamma*bw_util
    private final double alpha = 0.33;
    private final double beta = 0.33;
    private final double gamma = 0.33;
    private int actionableCycles = 0;

    @Override
    public Diagnosis<HostEntity> analyse(Map<HostEntity, Map<String, Double>> metrics, ReadSpace readSpace) {

        Map<HostEntity, LoadState> classification = new HashMap<>();
        Map<HostEntity, Double> costScores = new HashMap<>();

        //Iterate by Host
        for (var hostEntry : metrics.entrySet()) {

            // Calculate cost function
            HostEntity host = hostEntry.getKey();
            Map<String, Double> m = hostEntry.getValue();
            double cost = alpha * m.get("cpu_util")
                            + beta * m.get("ram_util")
                            + gamma * m.get("bw_util");
    
            costScores.put(host, cost);

        }

        // Compute mean and stddev across all hosts
        double mean = costScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
 
        double variance = costScores.values().stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average()
                .orElse(0.0);
 
        double stddev = Math.sqrt(variance);

        // Determind load
        for (var entry : costScores.entrySet()){

            HostEntity host = entry.getKey();
            double cost = entry.getValue();

            // Detemine load level
            if (cost > mean + stddev) {
                classification.put(host, LoadState.OVERLOADED);
                Log.printlnConcat( "Host #", host.getId(), " is overloaded.");
            } else if (cost < mean - stddev) {
                classification.put(host, LoadState.UNDERLOADED);
                Log.printlnConcat("Host #", host.getId(), " is underloaded.");
            } else {
                classification.put(host, LoadState.BALANCED);
                Log.printlnConcat( "Host #", host.getId(), " is balanced.");
            }

        }

        if (classification.containsValue(LoadState.OVERLOADED)){
            actionableCycles++;
        }


        return new Diagnosis<>(classification, costScores);

    }

    @Override
    public String inputGuid() {
        return "host-cpu-ram_bw";
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }
}
package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Analyser4 implements Analyser<Map<Integer, Map<HostEntity, Map<String, Double>>>, Diagnosis<HostEntity>> {

    private static final String METRIC = "bw_util";
    private static final double UPPER_THRESHOLD = 0.8;
    private static final double LOWER_THRESHOLD = 0.2;

    @Override
    public Diagnosis<HostEntity> analyse(Map<Integer, Map<HostEntity, Map<String, Double>>> metrics) {

        Map<HostEntity, LoadState> classification = new HashMap<>();
        Map<HostEntity, Integer> datacenterIds = new HashMap<>();
        Map<HostEntity, Double> values = new HashMap<>();

        //Iterate by datacenter
        for (Integer id : metrics.keySet()) {

            //Iterate by Host
            for (var entry : metrics.get(id).entrySet()) {

                HostEntity host = entry.getKey();
                double value = entry.getValue().get(METRIC);
                values.put(host, value);
                datacenterIds.put(host, id);

                // Detemine load level
                if (value > UPPER_THRESHOLD) {
                    classification.put(host, LoadState.OVERLOADED);
                    Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), " is overloaded.");
                } else if (value < LOWER_THRESHOLD) {
                    classification.put(host, LoadState.UNDERLOADED);
                    Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), " is underloaded.");
                } else {
                    classification.put(host, LoadState.BALANCED);
                    Log.printlnConcat("Datacenter #", id, "| Host #", host.getId(), " is balanced.");
                }
            }
        }
        
        return new Diagnosis<>(classification, values, datacenterIds);

    }

    @Override
    public Set<String> requiredMetrics() {
        return Set.of(METRIC);
    }
}
package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

public class Analyser2 implements Analyser<Map<HostEntity, Map<String, Double>>, Diagnosis<HostEntity>> {

    private static final String METRIC = "cpu_util";
    private static final double UPPER_THRESHOLD = 0.8;
    private static final double LOWER_THRESHOLD = 0.2;

    @Override
    public Diagnosis<HostEntity> analyse(Map<HostEntity, Map<String, Double>> metrics, ReadSpace readSpace) {

        Map<HostEntity, LoadState> classification = new HashMap<>();
        Map<HostEntity, Double> values = new HashMap<>();

        //Iterate by Host
        for (var entry : metrics.entrySet()) {

            HostEntity host = entry.getKey();
            double value = entry.getValue().get(METRIC);
            values.put(host, value);

            // Detemine load level
            if (value > UPPER_THRESHOLD) {
                classification.put(host, LoadState.OVERLOADED);
                Log.printlnConcat("Host #", host.getId(), " is overloaded.");
            } else if (value < LOWER_THRESHOLD) {
                classification.put(host, LoadState.UNDERLOADED);
                Log.printlnConcat( "Host #", host.getId(), " is underloaded.");
            } else {
                classification.put(host, LoadState.BALANCED);
                Log.printlnConcat("Host #", host.getId(), " is balanced.");
            }
        }


        return new Diagnosis<>(classification, values);

    }

    @Override
    public String inputGuid() {
        return METRIC;
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }
}
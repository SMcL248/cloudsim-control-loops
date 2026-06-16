package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Analyser1 implements Analyser<Map<GuestEntity, Map<String, Double>>, Diagnosis> {

    private static final String METRIC = "etc";

    @Override
    public Diagnosis analyse(Map<GuestEntity, Map<String, Double>> metrics) {

        Map<GuestEntity, Double> values = new HashMap<>();
        for (var entry : metrics.entrySet()) {
            values.put(entry.getKey(), entry.getValue().get(METRIC));
        }

        double mean = values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stddev = Math.sqrt(values.values().stream()
                .mapToDouble(v -> (v - mean) * (v - mean)).sum() / values.size());

        double smallest = Double.MAX_VALUE;
        double largest = 0;
        GuestEntity leastLoaded = null;
        GuestEntity mostLoaded = null;

        Map<GuestEntity, LoadState> classification = new HashMap<>();

        for (var entry : values.entrySet()) {
            GuestEntity vm = entry.getKey();
            double value = entry.getValue();

            if (value < smallest) {
                smallest = value;
                leastLoaded = vm;
            }

            if (value > mean + stddev) {
                classification.put(vm, LoadState.OVERLOADED);
                Log.printlnConcat("VM #", vm.getId(), " is overloaded.");
                if (value > largest) {
                    largest = value;
                    mostLoaded = vm;
                }
            } else if (value < mean - stddev) {
                classification.put(vm, LoadState.UNDERLOADED);
                Log.printlnConcat("VM #", vm.getId(), " is underloaded.");
            } else {
                classification.put(vm, LoadState.BALANCED);
                Log.printlnConcat("VM #", vm.getId(), " is balanced. ");
            }
        }

        return new Diagnosis(mean, stddev, values, classification, mostLoaded, leastLoaded);
    }

    @Override
    public Set<String> requiredMetrics() {
        return Set.of(METRIC);
    }
}
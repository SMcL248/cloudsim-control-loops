package org.cloudbus.cloudsim.examples;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class Analyser1 implements Analyser<Map<GuestEntity, Map<String, Double>>, Diagnosis<GuestEntity>> {

    private static final String METRIC = "etc";
    private int actionableCycles = 0;

    @Override
    public Diagnosis<GuestEntity> analyse(Map<GuestEntity, Map<String, Double>> metrics, ReadSpace readSpace) {


        Map<GuestEntity, Double> values = new HashMap<>();
        for (var entry : metrics.entrySet()) {
            values.put(entry.getKey(), entry.getValue().get(METRIC));
        }

        double mean = values.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stddev = Math.sqrt(values.values().stream()
                .mapToDouble(v -> (v - mean) * (v - mean)).sum() / values.size());

        Map<GuestEntity, LoadState> classification = new HashMap<>();

        for (var entry : values.entrySet()) {
            GuestEntity vm = entry.getKey();
            double value = entry.getValue();

            if (value > mean + stddev) {
                classification.put(vm, LoadState.OVERLOADED);
                actionableCycles++;
                Log.printlnConcat("VM #", vm.getId(), " is overloaded.");
            } else if (value < mean - stddev) {
                classification.put(vm, LoadState.UNDERLOADED);
                Log.printlnConcat("VM #", vm.getId(), " is underloaded.");
            } else {
                classification.put(vm, LoadState.BALANCED);
                Log.printlnConcat("VM #", vm.getId(), " is balanced.");
            }
        }

        if (classification.containsValue(LoadState.OVERLOADED)){
            actionableCycles++;
        }

        return new Diagnosis<>(classification, values);
    }

    @Override
    public String inputGuid() {
        return METRIC;
    }

    @Override
    public String outputGuid() {
        return "vm-loadstate";
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }
}

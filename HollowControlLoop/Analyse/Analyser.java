package org.cloudbus.cloudsim.examples;

import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;

public interface Analyser {

    Diagnosis analyse(Map<GuestEntity, Map<String, Double>> metrics);
    Set<String> requiredMetrics(); // e.g. {"etc"}
    
}

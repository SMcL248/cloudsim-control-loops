package org.cloudbus.cloudsim.examples;

import java.util.Map;
import java.util.Set;

import org.cloudbus.cloudsim.core.GuestEntity;

public interface Analyser<M, D> {

    D analyse(M metrics);
    Set<String> requiredMetrics(); // e.g. {"etc"}
    
}

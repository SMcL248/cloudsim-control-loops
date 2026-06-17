package org.cloudbus.cloudsim.examples;

import java.util.Set;


public interface Analyser<M, D> {

    D analyse(M metrics);
    Set<String> requiredMetrics(); // e.g. {"etc"}
    
}

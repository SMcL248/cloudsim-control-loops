package org.cloudbus.cloudsim.examples;

import java.util.Set;

public interface Monitor<M> {

    M observe(WorldState worldState); 
    Set<String> providedMetrics(); // e.g. {"etc"}, or {"ram_util", "etc"}

}
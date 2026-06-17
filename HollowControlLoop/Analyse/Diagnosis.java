package org.cloudbus.cloudsim.examples;

import java.util.Map;

public record Diagnosis<E>(
    Map<E, LoadState> classification,
    Map<E, Double> values,
    Map<E, Integer> datacenterIds
) {}
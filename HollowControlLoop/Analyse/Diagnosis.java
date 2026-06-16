package org.cloudbus.cloudsim.examples;

import java.util.Map;

import org.cloudbus.cloudsim.core.GuestEntity;

public record Diagnosis(
    double mean,
    double stddev,
    Map<GuestEntity, Double> values,        // the raw scalar each guest was classified on
    Map<GuestEntity, LoadState> classification,
    GuestEntity mostLoaded,                  // highest value among OVERLOADED guests (or null)
    GuestEntity leastLoaded
) {}

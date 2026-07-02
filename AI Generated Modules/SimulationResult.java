package org.cloudbus.cloudsim.examples;

public record SimulationResult(

    String monitorId,
    String analyserId,
    String plannerId,
    String executorId,
    int actionableCycles,
    int opportunityCycles,
    int actionsExecuted,
    double makespan,
    boolean compatible,
    double groundTruthAvgVariance

) {}

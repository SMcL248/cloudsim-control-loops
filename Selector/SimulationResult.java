package org.cloudbus.cloudsim.examples;

public record SimulationResult(

    String monitorId,
    String analyserId,
    String plannerId,
    String executorId,
    int actionableCycles,
    int opportunityCycles,
    int actionsProposed,
    int actionsExecuted,
    double makespan,
    boolean compatible,
    double groundTruthAvgVariance,
    double energy

) {

    public SimulationResult(
        String monitorId, 
        String analyserId, 
        String plannerId, 
        String executorId,
        int actionableCycles, 
        int opportunityCycles, 
        int actionsProposed, 
        int actionsExecuted, 
        double makespan, 
        boolean compatible, 
        double groundTruthAvgVariance){
        this(monitorId, analyserId, plannerId, executorId, actionableCycles, opportunityCycles,
             actionsProposed, actionsExecuted, makespan, compatible, groundTruthAvgVariance, Double.NaN);
    }
}

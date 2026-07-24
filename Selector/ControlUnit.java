package org.cloudbus.cloudsim.examples;

public interface ControlUnit {

    void observeAndAct(ActionSpace actionSpace);
    String getName();

    int getImbalanceCycles();
    int getOpportunityCycles();
    int getActionsProposed();
    int getActionsExecuted();

}
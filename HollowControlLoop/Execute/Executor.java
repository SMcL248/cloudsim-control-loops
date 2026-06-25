package org.cloudbus.cloudsim.examples;

public interface Executor<A> {
    boolean execute(A actions, ActionSpace actionSpace);
    int getActionsExecuted();
    String inputGuid();
}
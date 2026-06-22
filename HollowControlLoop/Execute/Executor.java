package org.cloudbus.cloudsim.examples;

public interface Executor<A> {
    boolean execute(A actions, ActionSpace actionSpace);
    String actionDescription();
    String inputGuid();
}
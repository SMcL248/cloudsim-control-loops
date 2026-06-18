package org.cloudbus.cloudsim.examples;

public interface Executor<A> {
    boolean execute(A migrations, ActionSpace actionSpace);
    String actionDescription();
}
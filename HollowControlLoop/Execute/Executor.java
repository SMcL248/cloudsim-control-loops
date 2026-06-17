package org.cloudbus.cloudsim.examples;

public interface Executor<A> {
    boolean execute(A migrations, SimulationContext context);
}
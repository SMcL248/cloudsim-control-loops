package org.cloudbus.cloudsim.examples;

import java.util.List;

public interface Executor<A> {
    boolean execute(A migrations, SimulationContext context);
}
package org.cloudbus.cloudsim.examples;

import java.util.List;

public interface Executor {
    boolean execute(List<MigrationPair> migrations, SimulationContext context);
}
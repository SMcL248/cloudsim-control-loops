package org.cloudbus.cloudsim.examples;

import java.util.List;

public interface Planner {
    List<MigrationPair> plan(Diagnosis diagnosis);
}
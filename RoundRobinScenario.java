package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.DatacenterBroker;

/**
 * Baseline scenario — standard DatacenterBroker allocates cloudlets
 * in round-robin order with no observation or migration.
 */
public class RoundRobinScenario extends BaseScenario {

    @Override
    protected DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker_RR");
    }

    @Override
    public String getName() {
        return "RoundRobin";
    }
}

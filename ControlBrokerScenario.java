package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.DatacenterBroker;

/**
 * Experimental scenario — ControlBroker allocates via least-occupied
 * and periodically observes and migrates overloaded cloudlets.
 */
public class ControlBrokerScenario extends BaseScenario {

    @Override
    protected DatacenterBroker createBroker() throws Exception {
        return new ControlBroker("Broker_CB", 20);
    }

    @Override
    public String getName() {
        return "ControlBroker";
    }
}

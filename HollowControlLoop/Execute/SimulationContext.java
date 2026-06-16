package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;

public interface SimulationContext {
    Integer getDatacenterFor(GuestEntity vm);
    void sendCloudlet(int datacenterId, double delay, Cloudlet cloudlet);
}
package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface SimulationContext {

    Integer getDatacenterFor(GuestEntity vm);
    void sendCloudlet(int datacenterId, double delay, Cloudlet cloudlet);
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    
}
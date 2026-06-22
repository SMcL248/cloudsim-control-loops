package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ActionSpace extends ReadSpace{

    void sendCloudlet(int datacenterId, double delay, Cloudlet cloudlet);
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    void moveCloudlet(Cloudlet cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId);
    void requestVmCreation(GuestEntity vm, int targetDatacenter);
    
}
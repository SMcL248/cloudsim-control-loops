package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ActionSpace extends ReadSpace{

    void sendCloudlet(int datacenterId, Cloudlet cloudlet);
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    void moveCloudlet(int cloudletId, int fromVmId, int toVmId);
    void requestVmCreation(GuestEntity vm, int targetDatacenter);

}
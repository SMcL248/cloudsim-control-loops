package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ActionSpace {

    Integer getDatacenterFor(GuestEntity vm);
    void sendCloudlet(int datacenterId, double delay, Cloudlet cloudlet);
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    void moveCloudlet(Cloudlet cloudlet, GuestEntity fromVm, GuestEntity toVm, int destDatacenterId);
    List<GuestEntity> getVmList();
    Integer getUserId();
    void requestVmCreation(GuestEntity vm, int targetDatacenter);
    List<HostEntity> getAllHosts();
    Double getNow();
    
}
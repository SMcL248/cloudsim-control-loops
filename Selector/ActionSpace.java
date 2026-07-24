package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ActionSpace extends ReadSpace{

    void sendCloudlet(int datacenterId, Cloudlet cloudlet);
    void requestCloudletCancellation(int datacenterId, Cloudlet cl);
    void moveCloudlet(int cloudletId, int fromVmId, int toVmId);
    void requestCloudletPause(int datacenterId, Cloudlet cl);
    void requestCloudletResume(int datacenterId, Cloudlet cl);
    void requestVmMigration(GuestEntity vm, HostEntity targetHost);
    GuestEntity requestVmCreation(int tierIndex, int sizeTierIndex, int datacenterId);
    void requestVmDestruction(GuestEntity vm);
    boolean requestMipsScaling(GuestEntity vm, double newValue);
    boolean requestRamScaling(GuestEntity vm, double newRam);
    boolean requestBwScaling(GuestEntity vm, double newBw);
    boolean requestPeAllocation(GuestEntity vm);
    boolean requestPeDeallocation(GuestEntity vm);
    void requestHostPowerDown(HostEntity host);
    void requestHostPowerUp(HostEntity host);
    
}
package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public interface ReadSpace {

    Integer getDatacenterFor(int vmId);
    List<GuestEntity> getVmList();
    Integer getUserId();
    List<HostEntity> getAllHosts();
    double getNow();
    
}
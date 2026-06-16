package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public record WorldState(
    
    List<GuestEntity> guests,
    List<HostEntity> hosts,
    double now

) {}
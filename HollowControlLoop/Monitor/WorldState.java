package org.cloudbus.cloudsim.examples;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public record WorldState(
    
    List<GuestEntity> guests,
    Map<Integer, List<HostEntity>> hosts,
    double now

) {}
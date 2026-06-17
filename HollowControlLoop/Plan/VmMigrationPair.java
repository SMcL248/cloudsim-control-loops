package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

public record VmMigrationPair (

    GuestEntity vm, 
    HostEntity targetHost

) {}


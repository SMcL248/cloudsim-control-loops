package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.core.GuestEntity;

public record MigrationPair (

    GuestEntity vm, 
    GuestEntity targetVm

) {}

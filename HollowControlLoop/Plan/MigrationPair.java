package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

public record MigrationPair (

    Cloudlet targetCloudlet, 
    GuestEntity fromVm,
    GuestEntity targetVm

) {}

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.core.GuestEntity;

record DestroyVmAction(

    GuestEntity vm,
    int datacenterId
    
) implements VmScalingAction {}

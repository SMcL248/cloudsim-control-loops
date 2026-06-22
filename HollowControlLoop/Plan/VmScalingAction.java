package org.cloudbus.cloudsim.examples;

sealed interface VmScalingAction permits CreateVmAction, DestroyVmAction {}

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.power.models.PowerModel;

public class PowerModelOff implements PowerModel {
    @Override
    public double getPower(double utilization) {
        return 0;
    }
}
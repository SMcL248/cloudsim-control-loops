package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.power.models.PowerModel;

public class PowerModelSpike implements PowerModel {
    private final PowerModel base;
    private final double multiplier;

    public PowerModelSpike(PowerModel base, double multiplier) {
        this.base = base;
        this.multiplier = multiplier;
    }

    @Override
    public double getPower(double utilization) {
        return base.getPower(utilization) * multiplier;
    }
}
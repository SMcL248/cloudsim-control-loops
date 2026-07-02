package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * analyser_v3 — CPU Demand Pressure, Fixed Thresholds
 *
 * Classifies each host by its CPU demand pressure (requested MIPS / total MIPS).
 * Unlike utilisation, demand can exceed 1.0 when VMs request more than the host
 * can supply. A demand ratio above 1.0 signals the host cannot satisfy its
 * workload (OVERLOADED). A ratio below 0.25 indicates the host is barely used
 * (UNDERLOADED).
 *
 * inputGuid  : host-cpu-demand
 * outputGuid : host-cpu-demand-loadstate
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final double OVER_THRESHOLD  = 1.00;
    private static final double UNDER_THRESHOLD = 0.25;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double demand = metrics[i];
            if (demand > OVER_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (demand < UNDER_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v3] Host ", i,
                    " cpu-demand=", demand, " state=", states[i]);
        }

        return states;
    }


    @Override
    public String inputGuid()  { return "host-cpu-demand"; }

    @Override
    public String outputGuid() { return "host-cpu-demand-loadstate"; }
}

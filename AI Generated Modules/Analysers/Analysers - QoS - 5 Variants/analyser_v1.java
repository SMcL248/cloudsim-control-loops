package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * analyser_v1 — CPU Utilisation, Fixed Thresholds
 *
 * Classifies each host by its CPU utilisation (used MIPS / total MIPS).
 * Thresholds are static: OVERLOADED above 0.80, UNDERLOADED below 0.20.
 * Simple and fast; suitable as a baseline policy.
 *
 * inputGuid  : host-cpu-util
 * outputGuid : host-cpu-util-loadstate
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final double OVER_THRESHOLD  = 0.80;
    private static final double UNDER_THRESHOLD = 0.20;


    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double util = metrics[i];
            if (util > OVER_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (util < UNDER_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v1] Host ", i,
                    " cpu-util=", util, " state=", states[i]);
        }

        return states;
    }


    @Override
    public String inputGuid()  { return "host-cpu-util"; }

    @Override
    public String outputGuid() { return "host-cpu-util-loadstate"; }
}

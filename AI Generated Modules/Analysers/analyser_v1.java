package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser variant 1 — Host-level CPU utilisation classifier.
 *
 * Reads a per-host CPU utilisation snapshot (double[] in [0.0, 1.0]) and
 * classifies each host as OVERLOADED, UNDERLOADED, or BALANCED using fixed
 * thresholds.
 *
 * Input  GUID : host-cpu
 * Output GUID : host-loadstate
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    /** CPU utilisation above this fraction → OVERLOADED */
    private static final double OVERLOAD_THRESHOLD  = 0.60;

    /** CPU utilisation below this fraction → UNDERLOADED */
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double cpuUtil = metrics[i];

            if (cpuUtil >= OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (cpuUtil <= UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v1] Host ", i,
                    " cpu_util=", String.format("%.3f", cpuUtil),
                    " -> ", states[i]);
        }

        return states;
    }

    @Override
    public String inputGuid() {
        return "host-cpu";
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }
}

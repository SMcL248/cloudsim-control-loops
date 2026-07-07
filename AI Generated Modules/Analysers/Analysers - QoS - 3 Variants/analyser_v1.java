package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v1 — CPU Utilisation, Static Threshold Analysis.
 *
 * Input metric : host CPU utilisation in range [0.0, 1.0]  (host-cpu-util)
 * Output       : LoadState per host                        (host-loadstate)
 *
 * A host is OVERLOADED  when utilisation exceeds HIGH_THRESHOLD.
 * A host is UNDERLOADED when utilisation falls below LOW_THRESHOLD.
 * Otherwise it is BALANCED.
 *
 * The actionable-cycle counter increments once per analyse() call in which
 * at least one host is OVERLOADED or UNDERLOADED.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final double HIGH_THRESHOLD = 0.80;
    private static final double LOW_THRESHOLD  = 0.20;

    private int actionableCycles = 0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];
        boolean actionable = false;

        for (int i = 0; i < metrics.length; i++) {
            double util = metrics[i];

            if (util > HIGH_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
                actionable = true;
            } else if (util < LOW_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
                actionable = true;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v1] Host[", i, "] cpu-util=",
                              String.format("%.4f", util), " -> ", states[i]);
        }

        if (actionable) {
            actionableCycles++;
            Log.printlnConcat(now, ": [analyser_v1] Actionable cycle detected (total=", actionableCycles, ")");
        }

        return states;
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }

    @Override
    public String inputGuid() {
        return "host-cpu-util";
    }

    @Override
    public String outputGuid() {
        return "host-cpu-util-loadstate";
    }
}

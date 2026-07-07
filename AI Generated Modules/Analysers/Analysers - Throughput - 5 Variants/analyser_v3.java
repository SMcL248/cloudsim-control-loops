package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v3 - VM Remaining Cloudlet Length, Normalised Hysteresis Bands
 *
 * Classifies each VM by its total remaining cloudlet length, first
 * min-max normalising the snapshot to [0, 1]. Rather than a single cutoff,
 * each state has separate "enter" and "release" thresholds so a VM must
 * cross further past a boundary to leave a state than it did to enter it:
 *
 *   enter OVERLOADED  : normalised > 0.70   (stays until < 0.55)
 *   enter UNDERLOADED : normalised < 0.30   (stays until > 0.45)
 *   otherwise BALANCED
 *
 * This damps rapid state flapping across successive control loop
 * invocations when a VM's load hovers near a boundary. Per-VM state is
 * retained between calls (indexed by array position); if the snapshot size
 * changes, history is reset. When all VMs carry an identical length (no
 * normalisation range), every VM is BALANCED.
 *
 * inputGuid  : vm-length
 * outputGuid : vm-length-loadstate
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final double ENTER_OVER   = 0.70;
    private static final double RELEASE_OVER = 0.55;
    private static final double ENTER_UNDER  = 0.30;
    private static final double RELEASE_UNDER = 0.45;

    private static LoadState[] previousStates = new LoadState[0];

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        if (previousStates.length != n) {
            previousStates = new LoadState[n];
            Arrays.fill(previousStates, LoadState.BALANCED);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : metrics) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;

        for (int i = 0; i < n; i++) {
            if (range == 0.0) {
                states[i] = LoadState.BALANCED;
            } else {
                double norm = (metrics[i] - min) / range;
                LoadState prev = previousStates[i];

                if (prev == LoadState.OVERLOADED) {
                    states[i] = (norm < RELEASE_OVER) ? LoadState.BALANCED : LoadState.OVERLOADED;
                } else if (prev == LoadState.UNDERLOADED) {
                    states[i] = (norm > RELEASE_UNDER) ? LoadState.BALANCED : LoadState.UNDERLOADED;
                } else if (norm > ENTER_OVER) {
                    states[i] = LoadState.OVERLOADED;
                } else if (norm < ENTER_UNDER) {
                    states[i] = LoadState.UNDERLOADED;
                } else {
                    states[i] = LoadState.BALANCED;
                }
            }

            Log.printlnConcat(now, ": [analyser_v3] VM ", i,
                    " length=", metrics[i], " state=", states[i]);
        }

        previousStates = states;
        return states;
    }

    @Override
    public String inputGuid() {
        return "vm-length";
    }

    @Override
    public String outputGuid() {
        return "vm-length-loadstate";
    }
}

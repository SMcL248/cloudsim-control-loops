package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser v4 - Host-level VM count classifier (relative to fleet mean).
 *
 * Level        : Host (level 2)
 * Metric       : Number of VMs currently assigned to each host.
 * Threshold    : Dynamic - relative to the mean VM count across all
 *                hosts in the current snapshot. A host carrying more
 *                than 1.5x the mean count is OVERLOADED (contention
 *                risk, hurts throughput); a host below 0.5x the mean
 *                (or empty) is UNDERLOADED (power-down candidate).
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_MULTIPLIER  = 1.5;
    private static final double UNDERLOAD_MULTIPLIER = 0.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double meanCount = (n > 0) ? sum / n : 0.0;

        for (int i = 0; i < n; i++) {
            double count = metrics[i];

            if (count <= 0.0) {
                states[i] = LoadState.UNDERLOADED;
            } else if (meanCount > 0.0 && count > OVERLOAD_MULTIPLIER * meanCount) {
                states[i] = LoadState.OVERLOADED;
            } else if (meanCount > 0.0 && count < UNDERLOAD_MULTIPLIER * meanCount) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v4] classified ", n,
                " hosts, mean-vm-count=", meanCount);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-vm-count";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-relative-mean";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}

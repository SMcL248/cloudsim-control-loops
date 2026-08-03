package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser v8 - VM-level utilisation variability classifier (percentile
 * rank).
 *
 * Level        : VM (level 3)
 * Metric       : Per-VM utilisation variability, e.g. a coefficient of
 *                variation over a recent observation window.
 * Threshold    : Dynamic - each VM is ranked by percentile position
 *                within the current snapshot's sorted values rather than
 *                compared to a fixed cut-off. The most variable/bursty
 *                20% of VMs are OVERLOADED (unpredictable, hurting
 *                throughput guarantees); the least variable/steadiest
 *                20% are UNDERLOADED (safe consolidation candidates).
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_PERCENTILE  = 0.80;
    private static final double UNDERLOAD_PERCENTILE = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        for (int i = 0; i < n; i++) {
            double percentileRank = rankOf(sorted, metrics[i]);

            if (percentileRank >= OVERLOAD_PERCENTILE) {
                states[i] = LoadState.OVERLOADED;
            } else if (percentileRank <= UNDERLOAD_PERCENTILE) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v8] classified ", n,
                " vms using percentile-rank variability bands");

        return states;
    }

    private double rankOf(double[] sortedValues, double value) {
        int n = sortedValues.length;
        if (n <= 1) {
            return 0.5;
        }
        int idx = Arrays.binarySearch(sortedValues, value);
        if (idx < 0) {
            idx = -(idx + 1);
        }
        return (double) idx / (double) (n - 1);
    }

    @Override
    public String inputSemantic() {
        return "vm-util-variability-cv";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-percentile-rank";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}

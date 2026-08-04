package org.cloudbus.cloudsim.examples;// always include

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

// Host-level analyser. Interprets metrics[i] as a per-host RAM utilisation
// fraction (used/total, in [0,1]) aligned with readSpace.getAllHosts().
// Classification uses quartiles of the observed batch: hosts in the top
// quartile of RAM usage are flagged OVERLOADED (little RAM headroom left
// to accept new or migrating VMs), hosts in the bottom quartile are
// flagged UNDERLOADED (plenty of spare RAM headroom).
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v2";
    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > q3) {
                result[i] = LoadState.OVERLOADED;
            } else if (v < q1) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " hosts via ram-utilization quartiles (q1="
                + q1 + ", q3=" + q3 + ")");

        return result;
    }

    // Nearest-rank percentile over an already-sorted array.
    private double percentile(double[] sorted, double p) {
        int n = sorted.length;
        if (n == 1) {
            return sorted[0];
        }
        int idx = (int) Math.round(p * (n - 1));
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= n) {
            idx = n - 1;
        }
        return sorted[idx];
    }

    @Override
    public String inputSemantic() {
        return "host-ram-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-load-classification-ram-quartile";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}

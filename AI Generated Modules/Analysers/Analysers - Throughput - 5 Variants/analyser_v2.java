package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v2 - VM Estimated Finish-Time, Median Absolute Deviation (MAD)
 *
 * Classifies each VM by its maximum estimated finish-time duration
 * (getEstimatedFinishTime() - time), i.e. how much simulation time remains
 * before the VM's current exec list drains. Uses the median and the median
 * absolute deviation (MAD) of the snapshot as a robust alternative to a
 * mean/stddev threshold, since a handful of very long-running VMs would
 * otherwise skew a mean-based cutoff:
 *
 *   OVERLOADED  : duration > median + K * MAD
 *   UNDERLOADED : duration < median - K * MAD
 *   BALANCED    : otherwise
 *
 * K defaults to 2.0. If MAD is 0 (e.g. most VMs share an identical
 * duration), all VMs are reported BALANCED to avoid false positives from a
 * degenerate spread.
 *
 * inputGuid  : vm-etc
 * outputGuid : vm-etc-loadstate
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final double K = 2.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double median = median(metrics);

        double[] deviations = new double[n];
        for (int i = 0; i < n; i++) deviations[i] = Math.abs(metrics[i] - median);
        double mad = median(deviations);

        double overThreshold  = median + K * mad;
        double underThreshold = median - K * mad;

        for (int i = 0; i < n; i++) {
            double duration = metrics[i];

            if (mad == 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (duration > overThreshold) {
                states[i] = LoadState.OVERLOADED;
            } else if (duration < underThreshold) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v2] VM ", i,
                    " etc=", duration, " median=", median, " mad=", mad,
                    " state=", states[i]);
        }

        return states;
    }

    /** Standard median of a copy of the given array (even length -> average of two middles). */
    private double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int len = sorted.length;
        if (len == 0) return 0.0;
        int mid = len / 2;
        return (len % 2 == 0) ? (sorted[mid - 1] + sorted[mid]) / 2.0 : sorted[mid];
    }

    @Override
    public String inputGuid() {
        return "vm-etc";
    }

    @Override
    public String outputGuid() {
        return "vm-etc-loadstate";
    }
}

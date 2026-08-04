package org.cloudbus.cloudsim.examples;// always include

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

// Host-level analyser. Interprets metrics[i] as a per-host bandwidth
// headroom fraction (available/total, in [0,1]) aligned with
// readSpace.getAllHosts(). Uses the median and median-absolute-deviation
// (MAD) of the observed batch, which is a more outlier-robust distribution
// summary than mean/stddev: hosts with headroom well below the median are
// OVERLOADED (bandwidth constrained), well above the median are
// UNDERLOADED (bandwidth to spare).
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v4";
    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double MAD_MULTIPLIER = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double median = median(metrics);

        double[] absDeviations = new double[n];
        for (int i = 0; i < n; i++) {
            absDeviations[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDeviations);

        double lowerBound = median - (MAD_MULTIPLIER * mad);
        double upperBound = median + (MAD_MULTIPLIER * mad);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v < lowerBound) {
                result[i] = LoadState.OVERLOADED;
            } else if (v > upperBound) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " hosts via bandwidth-headroom median+/-MAD (median="
                + median + ", mad=" + mad + ")");

        return result;
    }

    private double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 1) {
            return sorted[n / 2];
        }
        return (sorted[(n / 2) - 1] + sorted[n / 2]) / 2.0;
    }

    @Override
    public String inputSemantic() {
        return "host-bandwidth-headroom-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-load-classification-bandwidth-median-mad";
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

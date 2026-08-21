package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

/**
 * analyser_v3 - VM level, robust median/MAD classifier with asymmetric
 * multipliers.
 * Strategy: uses the median and median absolute deviation (MAD) of the
 * observed CPU utilisation fractions, which are robust to outliers unlike
 * mean/stddev. The overload multiplier is set higher than the underload
 * multiplier, reflecting that flagging a VM as overloaded (which may trigger
 * scaling/migration actions) should require stronger evidence than flagging
 * it as underloaded (a comparatively low-risk observation).
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final double MAD_CONSISTENCY_CONSTANT = 1.4826;
    private static final double OVERLOAD_MULTIPLIER = 2.0;
    private static final double UNDERLOAD_MULTIPLIER = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        double median = median(metrics);
        double mad = medianAbsoluteDeviation(metrics, median) * MAD_CONSISTENCY_CONSTANT;

        double upperBound = median + OVERLOAD_MULTIPLIER * mad;
        double lowerBound = median - UNDERLOAD_MULTIPLIER * mad;

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            if (mad <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            if (metrics[i] > upperBound) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (metrics[i] < lowerBound) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] classified ", n,
                " vms by median/MAD (median=", median, ", mad=", mad,
                ", upperBound=", upperBound, ", lowerBound=", lowerBound,
                ") -> overloaded=", overloadCount, ", underloaded=", underloadCount,
                ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    private double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return percentileFromSorted(sorted, 0.5);
    }

    private double medianAbsoluteDeviation(double[] values, double median) {
        double[] deviations = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            deviations[i] = Math.abs(values[i] - median);
        }
        Arrays.sort(deviations);
        return percentileFromSorted(deviations, 0.5);
    }

    private double percentileFromSorted(double[] sorted, double p) {
        if (sorted.length == 0) {
            return 0.0;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = p * (sorted.length - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        if (lowerIndex == upperIndex) {
            return sorted[lowerIndex];
        }
        double weight = rank - lowerIndex;
        return sorted[lowerIndex] * (1 - weight) + sorted[upperIndex] * weight;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpuutil-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadstate-cpuutil-mad";
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

package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

// Strategy: quantile / relative-rank classifier.
// Rather than parametric mean/stddev, this variant is rank-based and robust
// to outliers: it sorts the batch and uses the interquartile boundaries
// (Q1/Q3) as classification cutoffs. Cloudlets in the bottom quartile of
// progress relative to their peers are read as starved (OVERLOADED); those
// in the top quartile are read as coasting with spare capacity
// (UNDERLOADED).
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v3";

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-progress-ratio";
    private static final String OUTPUT_SEMANTIC = "cloudlet-progress-quantile-classification";

    private static final double LOWER_QUANTILE = 0.25;
    private static final double UPPER_QUANTILE = 0.75;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double q1 = percentile(sorted, LOWER_QUANTILE);
        double q3 = percentile(sorted, UPPER_QUANTILE);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (Double.isNaN(v)) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            if (v <= q1) {
                result[i] = LoadState.OVERLOADED;
            } else if (v >= q3) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] classified ", n,
                " cloudlets by quantile; q1=", q1, " q3=", q3);

        return result;
    }

    private double percentile(double[] sortedValues, double p) {
        int n = sortedValues.length;
        if (n == 1) {
            return sortedValues[0];
        }
        double rank = p * (n - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        if (lowerIndex == upperIndex) {
            return sortedValues[lowerIndex];
        }
        double weight = rank - lowerIndex;
        return sortedValues[lowerIndex] * (1 - weight) + sortedValues[upperIndex] * weight;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public String outputSemantic() {
        return OUTPUT_SEMANTIC;
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

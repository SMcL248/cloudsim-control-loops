package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser v10 - Cloudlet-level deadline slack classifier (quantile
 * bands).
 *
 * Level        : Cloudlet (level 4)
 * Metric       : Per-cloudlet estimated slack time (estimated finish
 *                time minus current simulation time); can be negative
 *                if a cloudlet is already running later than expected.
 * Threshold    : Dynamic - the 25th and 75th percentile slack values are
 *                computed from the current snapshot. Cloudlets at or
 *                below the 25th percentile (least slack, at risk of
 *                missing expectations) are OVERLOADED; cloudlets at or
 *                above the 75th percentile (ample slack) are
 *                UNDERLOADED, flagging spare throughput capacity that
 *                could be reclaimed to save power.
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final double LOWER_QUANTILE = 0.25;
    private static final double UPPER_QUANTILE = 0.75;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double lowerBound = percentile(sorted, LOWER_QUANTILE);
        double upperBound = percentile(sorted, UPPER_QUANTILE);

        for (int i = 0; i < n; i++) {
            if (metrics[i] <= lowerBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (metrics[i] >= upperBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v10] classified ", n,
                " cloudlets, slack-p25=", lowerBound, " slack-p75=", upperBound);

        return states;
    }

    private double percentile(double[] sortedValues, double fraction) {
        int n = sortedValues.length;
        if (n == 0) {
            return 0.0;
        }
        if (n == 1) {
            return sortedValues[0];
        }
        double pos = fraction * (n - 1);
        int lowIdx = (int) Math.floor(pos);
        int highIdx = (int) Math.ceil(pos);
        if (lowIdx == highIdx) {
            return sortedValues[lowIdx];
        }
        double weight = pos - lowIdx;
        return sortedValues[lowIdx] * (1.0 - weight) + sortedValues[highIdx] * weight;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-deadline-slack";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-quantile";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}

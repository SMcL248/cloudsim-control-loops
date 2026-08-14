package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 10 - Cloudlet-level estimated finish time, Tukey IQR
 * outlier fences.
 *
 * Strategy: computes each cloudlet's estimated finish time and applies
 * classic Tukey outlier fences (Q1 - 1.5*IQR, Q3 + 1.5*IQR) rather than a
 * tight statistical band. This deliberately only flags genuine outliers
 * (cloudlets whose finish time is far outside the bulk of the
 * distribution) and leaves the broad middle of the population BALANCED,
 * in contrast to the tighter bands used by other variants.
 *
 * Goal alignment: throughput-leaning. Cloudlets whose estimated finish
 * time is an extreme outlier on the high side are flagged OVERLOADED as
 * deadline/makespan risks worth prioritising; cloudlets finishing
 * unusually fast are flagged UNDERLOADED as low-risk/already ahead.
 *
 * Level: cloudlet (level 4). Input/output arrays are positionally aligned
 * with readSpace.getActiveCloudlets().
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final double FENCE_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double q1 = percentile(metrics, 25.0);
        double q3 = percentile(metrics, 75.0);
        double iqr = q3 - q1;

        double upperFence = q3 + (FENCE_MULTIPLIER * iqr);
        double lowerFence = q1 - (FENCE_MULTIPLIER * iqr);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperFence) {
                states[i] = LoadState.OVERLOADED;
            } else if (v < lowerFence) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v10] classified ", n,
                " cloudlets by estimated finish time, q1=", q1, " q3=", q3,
                " fences=[", lowerFence, ",", upperFence, "]");

        return states;
    }

    private double percentile(double[] values, double pct) {
        int n = values.length;
        if (n == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);
        if (n == 1) {
            return sorted[0];
        }
        double rank = (pct / 100.0) * (n - 1);
        int lowIndex = (int) Math.floor(rank);
        int highIndex = (int) Math.ceil(rank);
        if (lowIndex == highIndex) {
            return sorted[lowIndex];
        }
        double weight = rank - lowIndex;
        return sorted[lowIndex] + weight * (sorted[highIndex] - sorted[lowIndex]);
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-estimatedFinishTime: readSpace.getCloudletEstimatedFinishTime for the cloudlet on its current VM, absolute simulation time, one entry per cloudlet in readSpace.getActiveCloudlets() order";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState: OVERLOADED if estimatedFinishTime is above the Tukey upper fence (Q3 + 1.5*IQR) this cycle, UNDERLOADED if below the lower fence (Q1 - 1.5*IQR), else BALANCED";
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

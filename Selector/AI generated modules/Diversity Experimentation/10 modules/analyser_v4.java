package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.Arrays;
import java.util.List;

/*
 * Variant: analyser_v4
 * Level: CLOUDLET
 * Metric: completion fraction ((total - remaining) / total)
 * Strategy: Tukey IQR fences over how far each cloudlet has progressed
 * relative to its peers. A cloudlet with unusually little progress is
 * treated as OVERLOADED (it is starving for cycles); one with unusually
 * rapid progress is treated as UNDERLOADED (its host clearly has cycles to
 * spare).
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final double FENCE_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();
        LoadState[] states = new LoadState[metrics.length];

        double[] sorted = Arrays.copyOf(metrics, metrics.length);
        Arrays.sort(sorted);
        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double iqr = q3 - q1;
        double lowerFence = q1 - FENCE_MULTIPLIER * iqr;
        double upperFence = q3 + FENCE_MULTIPLIER * iqr;

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int i = 0; i < metrics.length; i++) {
            LoadState state;
            if (metrics[i] < lowerFence) {
                state = LoadState.OVERLOADED;
            } else if (metrics[i] > upperFence) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            states[i] = state;
            switch (state) {
                case OVERLOADED: overloaded++; break;
                case UNDERLOADED: underloaded++; break;
                default: balanced++; break;
            }
            if (state != LoadState.BALANCED && i < cloudlets.size()) {
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] cloudlet ", readSpace.getId(cloudlets.get(i)), " completion=", metrics[i], " -> ", state);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] IQR classification: q1=", q1, " q3=", q3, " -> ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    private double percentile(double[] sortedValues, double p) {
        int n = sortedValues.length;
        if (n == 0) {
            return 0.0;
        }
        if (n == 1) {
            return sortedValues[0];
        }
        double rank = p * (n - 1);
        int lowIndex = (int) Math.floor(rank);
        int highIndex = (int) Math.ceil(rank);
        if (lowIndex == highIndex) {
            return sortedValues[lowIndex];
        }
        double fraction = rank - lowIndex;
        return sortedValues[lowIndex] + fraction * (sortedValues[highIndex] - sortedValues[lowIndex]);
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-completion-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-iqr";
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

package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.Arrays;
import java.util.List;

/*
 * Variant: analyser_v8
 * Level: CLOUDLET
 * Metric: requested PE count
 * Strategy: trimmed-mean band over cloudlet PE demand. The most extreme 10%
 * of readings at each end of the sorted distribution are discarded before
 * the mean/std-dev band is computed, so a handful of unusually wide or
 * narrow cloudlets cannot distort the band used to judge EVERY cloudlet
 * (including the trimmed ones themselves, which are still classified
 * against the resulting robust band).
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final double TRIM_FRACTION = 0.10;
    private static final double K = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);
        int trimCount = (int) Math.floor(n * TRIM_FRACTION);
        int fromIndex = Math.min(trimCount, n / 2);
        int toIndexExclusive = Math.max(n - trimCount, fromIndex);

        double sum = 0.0;
        int trimmedCount = 0;
        for (int i = fromIndex; i < toIndexExclusive; i++) {
            sum += sorted[i];
            trimmedCount++;
        }

        if (trimmedCount == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] insufficient samples after trimming, defaulting all to BALANCED");
            Arrays.fill(states, LoadState.BALANCED);
            return states;
        }

        double mean = sum / trimmedCount;
        double sqDiff = 0.0;
        for (int i = fromIndex; i < toIndexExclusive; i++) {
            double d = sorted[i] - mean;
            sqDiff += d * d;
        }
        double std = Math.sqrt(sqDiff / trimmedCount);

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int i = 0; i < n; i++) {
            LoadState state;
            if (std < 1e-9) {
                state = LoadState.BALANCED;
            } else if (metrics[i] > mean + K * std) {
                state = LoadState.OVERLOADED;
            } else if (metrics[i] < mean - K * std) {
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
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] trimmed-mean PE-demand classification: mean=", mean, " std=", std, " -> ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-requested-pe-count";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-trimmed-mean";
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

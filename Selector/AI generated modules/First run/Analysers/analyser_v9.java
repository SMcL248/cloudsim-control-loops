package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser v9 - Cloudlet-level progress classifier (mean +/- stddev).
 *
 * Level        : Cloudlet (level 4)
 * Metric       : Per-cloudlet remaining-length ratio
 *                (remaining length / total length), range [0, 1], where
 *                values near 1 mean almost no progress has been made.
 * Threshold    : Dynamic - classifies relative to the mean and standard
 *                deviation of remaining-ratio across all active
 *                cloudlets this cycle. Cloudlets stalled further than
 *                one standard deviation above the mean are OVERLOADED
 *                (likely starved by host contention, hurting
 *                throughput); those more than one standard deviation
 *                below are UNDERLOADED (progressing unusually fast,
 *                spare capacity available).
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = (n > 0) ? sum / n : 0.0;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = (n > 0) ? Math.sqrt(sqDiffSum / n) : 0.0;

        double upperBound = mean + stdDev;
        double lowerBound = mean - stdDev;

        for (int i = 0; i < n; i++) {
            if (metrics[i] > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (metrics[i] < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v9] classified ", n,
                " cloudlets, mean-remaining-ratio=", mean, " stddev=", stdDev);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-ratio";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-zscore";
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

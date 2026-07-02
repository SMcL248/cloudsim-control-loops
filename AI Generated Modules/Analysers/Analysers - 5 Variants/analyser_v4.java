package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * analyser_v4 — CPU Demand Pressure, IQR Outlier Detection
 *
 * Classifies each host by its CPU demand pressure (requested MIPS / total MIPS).
 * Rather than fixed thresholds, this variant uses the interquartile range (IQR)
 * to identify statistical outliers across the current host population:
 *
 *   upperFence = Q3 + 1.5 * IQR  -> OVERLOADED
 *   lowerFence = Q1 - 1.5 * IQR  -> UNDERLOADED
 *
 * This is robust to skewed distributions and scales automatically with the
 * number of hosts. Requires at least 4 hosts; with fewer, all states are BALANCED.
 *
 * inputGuid  : host-cpu-demand
 * outputGuid : host-cpu-demand-loadstate
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int MIN_HOSTS_FOR_IQR = 4;

    private int actionableCycles = 0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        if (metrics.length < MIN_HOSTS_FOR_IQR) {
            for (int i = 0; i < metrics.length; i++) {
                states[i] = LoadState.BALANCED;
                Log.printlnConcat(now, ": [analyser_v4] Host ", i,
                        " cpu-demand=", metrics[i],
                        " state=BALANCED (insufficient hosts for IQR)");
            }
            return states;
        }

        double[] sorted = Arrays.copyOf(metrics, metrics.length);
        Arrays.sort(sorted);

        double q1         = interpolatedPercentile(sorted, 25.0);
        double q3         = interpolatedPercentile(sorted, 75.0);
        double iqr        = q3 - q1;
        double upperFence = q3 + 1.5 * iqr;
        double lowerFence = q1 - 1.5 * iqr;

        for (int i = 0; i < metrics.length; i++) {
            double demand = metrics[i];
            if (demand > upperFence) {
                states[i] = LoadState.OVERLOADED;
            } else if (demand < lowerFence) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v4] Host ", i,
                    " cpu-demand=", demand,
                    " q1=", q1, " q3=", q3, " iqr=", iqr,
                    " fence=[", lowerFence, ",", upperFence, "]",
                    " state=", states[i]);
        }

        return states;
    }

    /** Linear interpolation percentile on a pre-sorted array. */
    private double interpolatedPercentile(double[] sorted, double p) {
        double rank  = (p / 100.0) * (sorted.length - 1);
        int    lower = (int) rank;
        int    upper = Math.min(lower + 1, sorted.length - 1);
        return sorted[lower] + (rank - lower) * (sorted[upper] - sorted[lower]);
    }


    @Override
    public String inputGuid()  { return "host-cpu-demand"; }

    @Override
    public String outputGuid() { return "host-cpu-demand-loadstate"; }
}

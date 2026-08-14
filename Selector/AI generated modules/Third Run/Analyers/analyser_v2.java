package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 2 - Host-level power draw, median +/- MAD band.
 *
 * Strategy: classifies each host's instantaneous power draw (watts)
 * against the fleet median and median absolute deviation (MAD), scaled by
 * the standard normal-consistency constant 1.4826. Median/MAD is robust to
 * the handful of extreme outliers a small handful of always-on or
 * newly-powered hosts can produce, unlike a mean/std-dev band.
 *
 * Goal alignment: power-leaning. Hosts whose draw sits well above the
 * typical fleet draw are flagged as energy hotspots (candidates for
 * migration/consolidation away from); hosts well below typical draw are
 * flagged as near-idle power-down candidates.
 *
 * Level: host (level 2). Input/output arrays are positionally aligned with
 * readSpace.getAllHosts().
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final double MAD_TO_STDDEV = 1.4826;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double median = median(metrics);

        double[] absDevs = new double[n];
        for (int i = 0; i < n; i++) {
            absDevs[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDevs) * MAD_TO_STDDEV;

        double upperBound = median + mad;
        double lowerBound = median - mad;

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v2] classified ", n,
                " hosts by power draw watts, median=", median,
                " scaledMad=", mad, " band=[", lowerBound, ",", upperBound, "]");

        return states;
    }

    private double median(double[] values) {
        int n = values.length;
        if (n == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);
        int mid = n / 2;
        if (n % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        }
        return sorted[mid];
    }

    @Override
    public String inputSemantic() {
        return "host-powerWatts: instantaneous host power draw in watts as returned by readSpace.getHostPower, one entry per host in readSpace.getAllHosts() order";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState: OVERLOADED if powerWatts is above the fleet median plus scaled median-absolute-deviation this cycle, UNDERLOADED if below the median minus scaled MAD, else BALANCED";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}

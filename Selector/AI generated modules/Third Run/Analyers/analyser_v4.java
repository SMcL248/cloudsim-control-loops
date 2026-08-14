package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 4 - Host-level CPU utilisation, volatility-adaptive
 * band.
 *
 * Strategy: like variant 1, classifies against a mean-centred band, but
 * the band width scales with the fleet's coefficient of variation (CV =
 * stdDev / mean) this cycle. When the fleet is homogeneous (low CV) the
 * band tightens so real deviations are caught early; when the fleet is
 * volatile/bursty (high CV) the band widens so noise is not mistaken for
 * overload.
 *
 * Goal alignment: throughput-leaning. The adaptive width is aimed at
 * reducing false-positive overload flags during bursty workload arrivals,
 * which would otherwise trigger unnecessary (and throughput-costly)
 * corrective action.
 *
 * Level: host (level 2). Input/output arrays are positionally aligned with
 * readSpace.getAllHosts().
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final double MIN_K = 0.5;
    private static final double MAX_K = 2.0;

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

        double cv = (mean > 0.0) ? (stdDev / mean) : 0.0;
        double k = 0.5 + cv;
        if (k < MIN_K) {
            k = MIN_K;
        } else if (k > MAX_K) {
            k = MAX_K;
        }

        double upperBound = mean + (k * stdDev);
        double lowerBound = mean - (k * stdDev);

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

        Log.printlnConcat(now, ": [analyser_v4] classified ", n,
                " hosts by cpu util fraction, mean=", mean, " stdDev=", stdDev,
                " cv=", cv, " k=", k, " band=[", lowerBound, ",", upperBound, "]");

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-cpuUtilFraction: fraction of host total MIPS capacity currently allocated to guest VMs, range 0..1, one entry per host in readSpace.getAllHosts() order";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState: OVERLOADED/UNDERLOADED if cpuUtilFraction falls outside a mean-centred band whose half-width is k*stdDev, where k grows with this cycle's coefficient of variation (0.5..2.0), else BALANCED";
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

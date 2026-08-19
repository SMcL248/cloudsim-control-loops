package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import org.cloudbus.cloudsim.Log;

public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilFraction-instantaneous";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-medianMadRobust";

    // Scale factor that makes MAD comparable to a standard deviation under a
    // roughly normal distribution.
    private static final double MAD_SCALE = 1.4826;
    private static final double SPREAD_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] no VMs to classify.");
            return result;
        }

        double median = median(metrics);

        double[] absDeviations = new double[n];
        for (int i = 0; i < n; i++) {
            absDeviations[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDeviations) * MAD_SCALE;

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            if (mad < 1e-9) {
                // Every VM sits at (or very near) the median; no robust
                // outliers can be identified this round.
                result[i] = LoadState.BALANCED;
                continue;
            }
            double deviation = metrics[i] - median;
            if (deviation >= SPREAD_MULTIPLIER * mad) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (deviation <= -SPREAD_MULTIPLIER * mad) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] classified ", n,
                " VMs by robust median/MAD spread (median=", median, ", mad=", mad, "): ",
                overloadedCount, " overloaded, ", underloadedCount, " underloaded, ",
                (n - overloadedCount - underloadedCount), " balanced.");

        return result;
    }

    private double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        }
        return sorted[mid];
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

package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import org.cloudbus.cloudsim.Log;

/**
 * Variant 2 - VM level, population z-score thresholds.
 * Boundaries are derived from the observed population every call rather than
 * fixed constants: a VM more than Z standard deviations above the current
 * population mean is overloaded, more than Z below it is underloaded.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpu-utilization-instantaneous-fraction-of-vm-capacity";
    private static final String OUTPUT_SEMANTIC = "vm-load-classification-balanced-under-over";

    private static final double Z = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double mean = mean(metrics);
        double stdDev = stdDev(metrics, mean);

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            LoadState state;

            if (stdDev <= 0.0) {
                // No spread in the population this tick: nobody can be an
                // outlier relative to peers who are all reading the same.
                state = LoadState.BALANCED;
            } else if (util > mean + Z * stdDev) {
                state = LoadState.OVERLOADED;
            } else if (util < mean - Z * stdDev) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }

            states[i] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            else if (state == LoadState.UNDERLOADED) underloaded++;
            else balanced++;
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] z-score VM classification complete -> ",
                n, " vms, mean=", mean, ", stdDev=", stdDev, ", overloaded=", overloaded,
                ", underloaded=", underloaded, ", balanced=", balanced);

        return states;
    }

    private double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private double stdDev(double[] values, double mean) {
        if (values.length == 0) {
            return 0.0;
        }
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
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

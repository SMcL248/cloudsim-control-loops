package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// Host-level analyser. Interprets metrics[i] as a per-host MIPS utilisation
// fraction (used/total, in [0,1]) aligned with readSpace.getAllHosts().
// Classification uses the mean and standard deviation of the observed
// batch itself, so the overload/underload boundary adapts to whatever
// distribution of hosts is currently being monitored.
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v1";
    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double STDDEV_MULTIPLIER = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            return result;
        }

        double mean = 0.0;
        for (double v : metrics) {
            mean += v;
        }
        mean /= n;

        double variance = 0.0;
        for (double v : metrics) {
            variance += (v - mean) * (v - mean);
        }
        variance /= n;
        double stddev = Math.sqrt(variance);

        double upperBound = mean + (STDDEV_MULTIPLIER * stddev);
        double lowerBound = mean - (STDDEV_MULTIPLIER * stddev);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperBound) {
                result[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " hosts via mips-utilization mean+/-stddev (mean="
                + mean + ", stddev=" + stddev + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-mips-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-load-classification-mips-mean-std";
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

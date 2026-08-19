package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilFraction-normalized";
    private static final String OUTPUT_SEMANTIC = "host-loadState-zscoreRelative";

    private static final double Z_HIGH = 1.0;
    private static final double Z_LOW = -1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] no hosts to classify.");
            return result;
        }

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = sum / n;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double stdDev = Math.sqrt(sqDiffSum / n);

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            if (stdDev < 1e-9) {
                // No spread in the data this round; every host sits at the
                // mean, so there is nothing to distinguish them by.
                result[i] = LoadState.BALANCED;
                continue;
            }
            double z = (metrics[i] - mean) / stdDev;
            if (z >= Z_HIGH) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (z <= Z_LOW) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] classified ", n,
                " hosts by z-score around mean=", mean, ", stdDev=", stdDev, ": ",
                overloadedCount, " overloaded, ", underloadedCount, " underloaded, ",
                (n - overloadedCount - underloadedCount), " balanced.");

        return result;
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

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-mipsDemand-absolute";
    private static final String OUTPUT_SEMANTIC = "host-loadState-minMaxNormalized";

    private static final double OVERLOAD_NORMALIZED = 0.66;
    private static final double UNDERLOAD_NORMALIZED = 0.33;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] no hosts to classify.");
            return result;
        }

        double min = metrics[0];
        double max = metrics[0];
        for (double v : metrics) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        double range = max - min;

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            if (range < 1e-9) {
                // Every host is carrying an identical demand this round;
                // there is no range to normalize against.
                result[i] = LoadState.BALANCED;
                continue;
            }
            double normalized = (metrics[i] - min) / range;
            if (normalized >= OVERLOAD_NORMALIZED) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (normalized <= UNDERLOAD_NORMALIZED) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] classified ", n,
                " hosts by min-max normalized MIPS demand (min=", min, ", max=", max, "): ",
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

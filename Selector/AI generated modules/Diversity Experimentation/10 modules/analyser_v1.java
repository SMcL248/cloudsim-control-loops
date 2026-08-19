package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilFraction-normalized";
    private static final String OUTPUT_SEMANTIC = "host-loadState-fixedThreshold";

    private static final double OVERLOAD_THRESHOLD = 0.75;
    private static final double UNDERLOAD_THRESHOLD = 0.25;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (util >= OVERLOAD_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (util <= UNDERLOAD_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] classified ", n,
                " hosts using fixed thresholds (overload>=", OVERLOAD_THRESHOLD,
                ", underload<=", UNDERLOAD_THRESHOLD, "): ", overloadedCount,
                " overloaded, ", underloadedCount, " underloaded, ",
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

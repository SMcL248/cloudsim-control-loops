package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;

public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remainingLengthFraction-progressRatio";
    private static final String OUTPUT_SEMANTIC = "cloudlet-loadState-fixedThreshold";

    // Fraction of original cloudlet length still remaining.
    private static final double OVERLOAD_THRESHOLD = 0.70;
    private static final double UNDERLOAD_THRESHOLD = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int overloadedCount = 0;
        int underloadedCount = 0;

        for (int i = 0; i < n; i++) {
            double remainingFraction = metrics[i];
            // A cloudlet with most of its work still remaining is treated as
            // OVERLOADED: it is at the greatest risk of starvation or loss.
            // A cloudlet close to completion is treated as UNDERLOADED: it is
            // still consuming resources but no longer contending heavily.
            if (remainingFraction >= OVERLOAD_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloadedCount++;
            } else if (remainingFraction <= UNDERLOAD_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloadedCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] classified ", n,
                " cloudlets by fixed remaining-length thresholds (overload>=",
                OVERLOAD_THRESHOLD, ", underload<=", UNDERLOAD_THRESHOLD, "): ",
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

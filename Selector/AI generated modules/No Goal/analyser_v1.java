package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v1 - Host level, fixed absolute threshold classifier.
 * Strategy: naive/baseline approach. Classifies each host's CPU utilisation
 * fraction against two fixed constants. No use of the observed distribution.
 * Serves as a simple baseline against which distribution-aware variants can
 * be compared.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final double OVERLOAD_THRESHOLD = 0.80;
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (util > OVERLOAD_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (util < UNDERLOAD_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] classified ", n,
                " hosts using fixed thresholds (over=", OVERLOAD_THRESHOLD,
                ", under=", UNDERLOAD_THRESHOLD, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-cpuutil-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-loadstate-cpuutil";
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

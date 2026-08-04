package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// VM-level analyser. Interprets metrics[i] as a per-VM CPU utilisation
// fraction (in [0,1]) aligned with readSpace.getVmList(). Uses simple
// fixed absolute thresholds - a straightforward textbook classification
// with no dependency on the shape of the current batch, useful as a
// stable baseline for throughput/service-quality goals.
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v5";
    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final double OVERLOAD_THRESHOLD = 0.8;
    private static final double UNDERLOAD_THRESHOLD = 0.2;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > OVERLOAD_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
            } else if (v < UNDERLOAD_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " vms via cpu-utilization fixed thresholds (high="
                + OVERLOAD_THRESHOLD + ", low=" + UNDERLOAD_THRESHOLD + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-classification-cpu-fixed-threshold";
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

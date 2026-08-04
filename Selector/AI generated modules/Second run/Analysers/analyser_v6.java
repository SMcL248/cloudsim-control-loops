package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// VM-level analyser. Interprets metrics[i] as a per-VM mean utilisation
// fraction (e.g. sourced from readSpace.getVmUtilizationMean, in [0,1])
// aligned with readSpace.getVmList(). Uses min-max normalisation across
// the observed batch: each value is rescaled onto [0,1] relative to the
// current min/max spread, then a fixed cutoff on the normalised scale
// decides overload/underload. This differs from raw fixed thresholds
// (analyser_v5) because it reacts to how spread out the current batch is.
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v6";
    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final double UPPER_NORMALIZED_CUTOFF = 0.7;
    private static final double LOWER_NORMALIZED_CUTOFF = 0.3;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n == 0) {
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

        for (int i = 0; i < n; i++) {
            double normalized;
            if (range <= 0.0) {
                // All VMs report the same value: nothing distinguishes them.
                normalized = 0.5;
            } else {
                normalized = (metrics[i] - min) / range;
            }

            if (normalized > UPPER_NORMALIZED_CUTOFF) {
                result[i] = LoadState.OVERLOADED;
            } else if (normalized < LOWER_NORMALIZED_CUTOFF) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " vms via utilization min-max normalization (min="
                + min + ", max=" + max + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-mean-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-classification-utilization-minmax-normalized";
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

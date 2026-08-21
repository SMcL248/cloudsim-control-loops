package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

/**
 * Variant 8 - VM level - Min-max range normalisation.
 * Assumes the input metric is the per-VM ratio of effective
 * throughput to requested MIPS (delivered / provisioned capacity).
 * Each value is linearly rescaled into [0,1] using the observed
 * minimum and maximum of the snapshot, then compared against fixed
 * cut points on that normalised scale. This differs from a rank-based
 * approach: two VMs close in raw value stay close after scaling,
 * whereas rank-based classification only cares about ordering.
 */
public class analyser_v8 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final double UPPER_CUT = 0.70;
    private static final double LOWER_CUT = 0.30;

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
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double range = max - min;

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            if (range <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double normalised = (metrics[i] - min) / range;
            if (normalised > UPPER_CUT) {
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (normalised < LOWER_CUT) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        List<GuestEntity> vms = readSpace.getVmList();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v8] classified ", n,
            " of ", vms.size(), " VMs via min-max normalisation (min=", min,
            ", max=", max, ") -> overloaded=", overloaded, ", underloaded=",
            underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-effectiveThroughputRatio-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState-minMaxNormalised";
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

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;

import java.util.Arrays;


// Host-level bandwidth utilisation classifier using IQR outlier fencing; only statistical outliers are flagged.
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-bandwidth_utilisation_ratio-fraction_of_total_bandwidth_allocated";
    private static final String OUTPUT_SEMANTIC = "host-load_state-iqr_outlier_fence_classification_of_bandwidth_utilisation";

    private static final double FENCE_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);
        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double iqr = q3 - q1;
        double upperFence = q3 + FENCE_MULTIPLIER * iqr;
        double lowerFence = q1 - FENCE_MULTIPLIER * iqr;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (n < 4 || iqr <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (util >= upperFence) {
                states[i] = LoadState.OVERLOADED;
            } else if (util <= lowerFence) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] classified ", n,
                " hosts by IQR outlier fencing of bandwidth utilisation (q1=", q1, ", q3=", q3, ")");

        return states;
    }

    private double percentile(double[] sortedValues, double p) {
        int n = sortedValues.length;
        if (n == 0) return 0.0;
        int idx = (int) Math.round(p * (n - 1));
        idx = Math.max(0, Math.min(n - 1, idx));
        return sortedValues[idx];
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

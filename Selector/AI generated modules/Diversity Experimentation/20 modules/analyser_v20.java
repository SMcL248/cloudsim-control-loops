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


// Cloudlet-level progress-rate classifier using a std-scaled dead-band anchored at a fixed 0.5 expected-progress point.
public class analyser_v20 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-progress_rate-fraction_of_total_instruction_length_already_processed";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-std_scaled_dead_band_classification_of_progress_rate";

    private static final double ANCHOR = 0.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double mean = 0.0;
        for (double v : metrics) mean += v;
        mean = n == 0 ? 0.0 : mean / n;

        double sumSq = 0.0;
        for (double v : metrics) sumSq += (v - mean) * (v - mean);
        double std = n == 0 ? 0.0 : Math.sqrt(sumSq / n);

        double upperBound = ANCHOR + std;
        double lowerBound = ANCHOR - std;

        for (int i = 0; i < n; i++) {
            double progress = metrics[i];
            if (std <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (progress >= upperBound) {
                states[i] = LoadState.UNDERLOADED;
            } else if (progress <= lowerBound) {
                states[i] = LoadState.OVERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v20] classified ", n,
                " cloudlets by std-scaled dead-band around fixed 0.5 progress anchor (std=", std, ")");

        return states;
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

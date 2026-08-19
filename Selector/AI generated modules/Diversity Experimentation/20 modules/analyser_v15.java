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


// Cloudlet-level classifier of remaining-length fraction using fixed, hand-tuned thresholds.
public class analyser_v15 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remaining_length_fraction-fraction_of_total_instruction_length_still_unprocessed";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-fixed_threshold_classification_of_remaining_work_fraction";

    private static final double OVERLOAD_THRESHOLD = 0.80;
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double remainingFraction = metrics[i];
            if (remainingFraction >= OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (remainingFraction <= UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v15] classified ", metrics.length,
                " cloudlets by fixed thresholds of remaining work fraction (overload>=", OVERLOAD_THRESHOLD,
                ", underload<=", UNDERLOAD_THRESHOLD, ")");

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

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


// Host-level CPU utilisation classifier using fixed, hand-tuned thresholds.
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpu_utilisation_ratio-fraction_of_total_mips_currently_in_use";
    private static final String OUTPUT_SEMANTIC = "host-load_state-fixed_threshold_classification_of_cpu_utilisation";

    private static final double OVERLOAD_THRESHOLD = 0.80;
    private static final double UNDERLOAD_THRESHOLD = 0.30;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double util = metrics[i];
            if (util >= OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (util <= UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] classified ", metrics.length,
                " hosts by fixed CPU utilisation thresholds (overload>=", OVERLOAD_THRESHOLD,
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

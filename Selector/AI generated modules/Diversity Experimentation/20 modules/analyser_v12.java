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


// VM-level throughput-efficiency classifier (effective/requested MIPS) using fixed bands.
public class analyser_v12 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-throughput_efficiency-ratio_of_effective_to_requested_mips";
    private static final String OUTPUT_SEMANTIC = "vm-load_state-fixed_band_classification_of_throughput_efficiency";

    private static final double STARVED_THRESHOLD = 0.60;
    private static final double SLACK_THRESHOLD = 0.98;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double efficiency = metrics[i];
            if (efficiency <= STARVED_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (efficiency >= SLACK_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v12] classified ", metrics.length,
                " VMs by throughput efficiency bands (starved<=", STARVED_THRESHOLD,
                ", slack>=", SLACK_THRESHOLD, ")");

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

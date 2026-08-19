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


// VM-level classifier of requested MIPS as a share of the hosting PE capacity, using fixed thresholds.
public class analyser_v11 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-requested_mips_share-fraction_of_hosting_pe_capacity_requested_by_vm";
    private static final String OUTPUT_SEMANTIC = "vm-load_state-fixed_threshold_classification_of_host_capacity_share";

    private static final double OVERLOAD_THRESHOLD = 0.90;
    private static final double UNDERLOAD_THRESHOLD = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        LoadState[] states = new LoadState[metrics.length];

        for (int i = 0; i < metrics.length; i++) {
            double share = metrics[i];
            if (share >= OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (share <= UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v11] classified ", metrics.length,
                " VMs by fixed thresholds of host capacity share (overload>=", OVERLOAD_THRESHOLD,
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

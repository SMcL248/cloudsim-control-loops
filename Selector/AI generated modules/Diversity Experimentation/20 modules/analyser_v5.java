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


// Host-level guest-count classifier using fixed integer occupancy bands.
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-guest_count-number_of_vms_currently_hosted";
    private static final String OUTPUT_SEMANTIC = "host-load_state-fixed_integer_band_classification_of_guest_count";

    private static final long EMPTY_BAND = 0;
    private static final long CROWDED_BAND = 5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        for (int i = 0; i < n; i++) {
            long guestCount = Math.round(metrics[i]);
            if (guestCount >= CROWDED_BAND) {
                states[i] = LoadState.OVERLOADED;
            } else if (guestCount <= EMPTY_BAND) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] classified ", n,
                " hosts by fixed guest-count bands (empty<=", EMPTY_BAND, ", crowded>=", CROWDED_BAND, ")");

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

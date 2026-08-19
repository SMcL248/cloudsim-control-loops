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


// Host-level available-MIPS-headroom classifier using a quartile split (Q1/Q3) of the observed distribution.
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-available_mips_headroom-unallocated_mips_capacity_of_host_pes";
    private static final String OUTPUT_SEMANTIC = "host-load_state-quartile_classification_of_mips_headroom";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);
        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);

        for (int i = 0; i < n; i++) {
            double headroom = metrics[i];
            if (n < 4) {
                states[i] = LoadState.BALANCED;
            } else if (headroom <= q1) {
                states[i] = LoadState.OVERLOADED;
            } else if (headroom >= q3) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] classified ", n,
                " hosts by quartile split of available MIPS headroom (q1=", q1, ", q3=", q3, ")");

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

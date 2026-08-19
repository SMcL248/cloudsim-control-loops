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


// Cloudlet-level classifier of total instruction length using a tertile (equal-population thirds) rank split.
public class analyser_v16 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-total_length-total_instruction_count_of_cloudlet_in_million_instructions";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-tertile_rank_classification_of_relative_cloudlet_size";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);

        for (int i = 0; i < n; i++) {
            double rank = rankFraction(sorted, metrics[i]);
            if (n < 3) {
                states[i] = LoadState.BALANCED;
            } else if (rank > (2.0 / 3.0)) {
                states[i] = LoadState.OVERLOADED;
            } else if (rank <= (1.0 / 3.0)) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v16] classified ", n,
                " cloudlets into tertiles by relative workload size");

        return states;
    }

    private double rankFraction(double[] sortedValues, double value) {
        int n = sortedValues.length;
        if (n <= 1) return 0.5;
        int countAtOrBelow = 0;
        for (double v : sortedValues) {
            if (v <= value) countAtOrBelow++;
        }
        return (double) countAtOrBelow / n;
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

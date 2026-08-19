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


// Cloudlet-level progress-rate classifier using percentile rank against the observed population (bottom/top 15%).
public class analyser_v17 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-progress_rate-fraction_of_total_instruction_length_already_processed";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-percentile_rank_classification_of_progress_rate";

    private static final double UPPER_RANK = 0.85;
    private static final double LOWER_RANK = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);

        for (int i = 0; i < n; i++) {
            double rank = rankFraction(sorted, metrics[i]);
            if (n < 5) {
                states[i] = LoadState.BALANCED;
            } else if (rank <= LOWER_RANK) {
                states[i] = LoadState.OVERLOADED;
            } else if (rank >= UPPER_RANK) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v17] classified ", n,
                " cloudlets by percentile rank of progress rate (lower_rank<=", LOWER_RANK,
                ", upper_rank>=", UPPER_RANK, ")");

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

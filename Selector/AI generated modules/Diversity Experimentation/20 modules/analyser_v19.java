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


// Cloudlet-level remaining-backlog classifier using a symmetric population z-score (mean +/- 1 std).
public class analyser_v19 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remaining_length-absolute_unprocessed_instruction_count_in_million_instructions";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-population_zscore_classification_of_remaining_backlog";

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

        double upperBound = mean + std;
        double lowerBound = mean - std;

        for (int i = 0; i < n; i++) {
            double backlog = metrics[i];
            if (std <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (backlog >= upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (backlog <= lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v19] classified ", n,
                " cloudlets by population z-score of remaining backlog (mean=", mean, ", std=", std, ")");

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

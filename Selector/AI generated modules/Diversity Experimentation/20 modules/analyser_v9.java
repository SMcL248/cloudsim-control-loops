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


// VM-level rolling-mean-utilisation classifier using a symmetric population z-score (mean +/- 1 std).
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-utilisation_mean-thirty_reading_rolling_average_of_cpu_utilisation";
    private static final String OUTPUT_SEMANTIC = "vm-load_state-population_zscore_classification_of_mean_utilisation";

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
            double util = metrics[i];
            if (std <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (util >= upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (util <= lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] classified ", n,
                " VMs by population z-score of mean utilisation (mean=", mean, ", std=", std, ")");

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

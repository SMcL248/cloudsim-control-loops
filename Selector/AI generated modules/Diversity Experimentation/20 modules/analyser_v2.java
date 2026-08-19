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


// Host-level power-draw classifier using a symmetric population z-score (mean +/- 1 std).
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-power_draw-current_watts_consumed_by_host_pes";
    private static final String OUTPUT_SEMANTIC = "host-load_state-population_zscore_classification_of_power_draw";

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
            double power = metrics[i];
            if (std <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (power >= upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (power <= lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] classified ", n,
                " hosts by population z-score of power draw (mean=", mean, ", std=", std, ")");

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

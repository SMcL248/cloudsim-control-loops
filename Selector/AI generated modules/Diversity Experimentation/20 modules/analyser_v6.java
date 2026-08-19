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


// Host-level power-ratio classifier using a dead-band around the range midpoint (fixed 10% band width).
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-power_ratio_to_max-fraction_of_max_power_draw_currently_consumed";
    private static final String OUTPUT_SEMANTIC = "host-load_state-dead_band_hysteresis_classification_around_data_midpoint";

    private static final double BAND_FRACTION = 0.10;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : metrics) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        if (n == 0) {
            min = 0.0;
            max = 0.0;
        }

        double midpoint = (min + max) / 2.0;
        double band = (max - min) * BAND_FRACTION;
        double upperBound = midpoint + band;
        double lowerBound = midpoint - band;

        for (int i = 0; i < n; i++) {
            double ratio = metrics[i];
            if (max - min <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (ratio >= upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (ratio <= lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] classified ", n,
                " hosts by dead-band hysteresis around midpoint of power ratio (midpoint=", midpoint,
                ", band=", band, ")");

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

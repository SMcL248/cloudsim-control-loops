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


// Cloudlet-level workload-density (length per PE) classifier using median +/- scaled median-absolute-deviation.
public class analyser_v18 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-length_per_pe-total_instruction_length_divided_by_requested_pe_count";
    private static final String OUTPUT_SEMANTIC = "cloudlet-load_state-median_mad_classification_of_workload_density";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);
        double med = median(sorted);

        double[] absDev = new double[n];
        for (int i = 0; i < n; i++) absDev[i] = Math.abs(metrics[i] - med);
        double[] sortedDev = absDev.clone();
        Arrays.sort(sortedDev);
        double madScaled = median(sortedDev) * 1.4826;

        double upperBound = med + madScaled;
        double lowerBound = med - madScaled;

        for (int i = 0; i < n; i++) {
            double density = metrics[i];
            if (madScaled <= 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (density >= upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (density <= lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v18] classified ", n,
                " cloudlets by median/MAD of workload density (median=", med, ", scaled_mad=", madScaled, ")");

        return states;
    }

    private double median(double[] sortedValues) {
        int n = sortedValues.length;
        if (n == 0) return 0.0;
        if (n % 2 == 1) return sortedValues[n / 2];
        return (sortedValues[n / 2 - 1] + sortedValues[n / 2]) / 2.0;
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

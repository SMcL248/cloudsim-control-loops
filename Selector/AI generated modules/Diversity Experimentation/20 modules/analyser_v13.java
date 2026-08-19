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


// VM-level CPU utilisation classifier using an asymmetric z-score: aggressive overload cutoff, conservative underload cutoff.
public class analyser_v13 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpu_utilisation-instantaneous_fraction_of_vm_mips_in_use";
    private static final String OUTPUT_SEMANTIC = "vm-load_state-asymmetric_zscore_classification_of_cpu_utilisation";

    private static final double OVERLOAD_SIGMA = 0.5;
    private static final double UNDERLOAD_SIGMA = 1.5;

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

        double upperBound = mean + OVERLOAD_SIGMA * std;
        double lowerBound = mean - UNDERLOAD_SIGMA * std;

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

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v13] classified ", n,
                " VMs by asymmetric z-score of CPU utilisation (overload_sigma=", OVERLOAD_SIGMA,
                ", underload_sigma=", UNDERLOAD_SIGMA, ")");

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

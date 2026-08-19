package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.Arrays;
import java.util.List;

/*
 * Variant: analyser_v3
 * Level: VM
 * Metric: CPU utilisation fraction
 * Strategy: robust classification using the sample median and the Median
 * Absolute Deviation (MAD) of the current utilisation snapshot, rather than
 * the mean/std-dev band used elsewhere. MAD is far less sensitive to a
 * handful of extreme VMs than a standard-deviation band, so this variant
 * should stay stable when a few VMs spike.
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final double MAD_SCALE = 1.4826; // normal-consistency scaling factor
    private static final double K = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        LoadState[] states = new LoadState[metrics.length];

        double median = median(metrics);
        double[] absDevs = new double[metrics.length];
        for (int i = 0; i < metrics.length; i++) {
            absDevs[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDevs) * MAD_SCALE;
        double upper = median + K * mad;
        double lower = median - K * mad;

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int i = 0; i < metrics.length; i++) {
            GuestEntity vm = vms.get(i);
            LoadState state;
            if (mad < 1e-9) {
                state = Math.abs(metrics[i] - median) < 1e-9 ? LoadState.BALANCED
                        : (metrics[i] > median ? LoadState.OVERLOADED : LoadState.UNDERLOADED);
            } else if (metrics[i] > upper) {
                state = LoadState.OVERLOADED;
            } else if (metrics[i] < lower) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            states[i] = state;
            switch (state) {
                case OVERLOADED: overloaded++; break;
                case UNDERLOADED: underloaded++; break;
                default: balanced++; break;
            }
            if (state != LoadState.BALANCED) {
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] vm ", readSpace.getId(vm), " util=", metrics[i], " -> ", state);
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v3] median-MAD classification: median=", median, " mad=", mad, " -> ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    private double median(double[] values) {
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n == 0) {
            return 0.0;
        }
        if (n % 2 == 1) {
            return sorted[n / 2];
        }
        return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-median-mad";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}

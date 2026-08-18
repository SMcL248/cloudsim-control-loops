package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Variant 5 - VM level, per-entity adaptive baseline (rolling mean +/- MAD).
 * Unlike variant 2's population-wide z-score, each VM is judged only against
 * its own recent history: a VM whose current reading strays far from its own
 * 30-reading rolling mean (in MAD units) is flagged, even if that reading is
 * perfectly ordinary compared to its neighbours.
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpu-utilization-instantaneous-fraction-of-vm-capacity";
    private static final String OUTPUT_SEMANTIC = "vm-load-classification-balanced-under-over";

    private static final double MAD_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        List<GuestEntity> vms = readSpace.getVmList();

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);
            double vmMips = readSpace.getVmMips(vm);

            // getVmUtilizationMean() returns a fraction of VM capacity, while
            // the rolling MAD is on a MIPS scale. Scale the mean (and current
            // reading) up by the VM's MIPS rating before comparing.
            double currentMips = metrics[i] * vmMips;
            double meanMips = readSpace.getVmUtilizationMean(vm) * vmMips;
            double mad = readSpace.getVmUtilizationMad(vm);

            LoadState state;
            if (mad <= 0.0) {
                // No recorded deviation yet (e.g. VM just instantiated): no
                // baseline exists for this VM to be an outlier against.
                state = LoadState.BALANCED;
            } else if (currentMips > meanMips + MAD_MULTIPLIER * mad) {
                state = LoadState.OVERLOADED;
            } else if (currentMips < meanMips - MAD_MULTIPLIER * mad) {
                state = LoadState.UNDERLOADED;
            } else {
                state = LoadState.BALANCED;
            }

            states[i] = state;
            if (state == LoadState.OVERLOADED) overloaded++;
            else if (state == LoadState.UNDERLOADED) underloaded++;
            else balanced++;
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] per-VM adaptive-baseline classification complete -> ",
                n, " vms, overloaded=", overloaded, ", underloaded=", underloaded, ", balanced=", balanced);

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

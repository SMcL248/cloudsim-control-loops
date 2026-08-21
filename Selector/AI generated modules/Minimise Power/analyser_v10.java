package org.cloudbus.cloudsim.examples;// always include

// Import whats needed
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

/**
 * analyser_v10
 *
 * STRATEGY: Equal-width range binning. Level: CLOUDLET (4). Metric: cloudlet remaining
 * work ratio (remaining length / total length, 0 = about to finish, 1 = just started).
 *
 * Rationale: this is the only cloudlet-level variant, and the only variant that bins by
 * equal-width slices of the observed min..max range (as opposed to v4's equal-COUNT rank
 * buckets or v6's gap-based clustering). Cloudlets with little work remaining are
 * UNDERLOADED - a signal that the VM/host currently hosting them is about to free up
 * capacity, an imminent power-down or consolidation opportunity. Cloudlets with most of
 * their work still ahead are OVERLOADED - a signal that whatever host is running them
 * will stay busy, and power-hungry, for a while yet. This gives the planner a forward-
 * looking, workload-completion view of power demand rather than a purely current-
 * utilisation view (v1-v9).
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1400;
    private static final int OUTPUT_GUID = 2400;
    private static final String INPUT_SEMANTIC = "cloudlet-remainingWorkRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "cloudlet-loadState-equalWidthBin";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        double min = metrics[0];
        double max = metrics[0];
        for (double v : metrics) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        double range = max - min;
        if (range == 0.0) {
            // All cloudlets have identical remaining-work ratio right now.
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
            }
            balanced = n;
        } else {
            double binWidth = range / 3.0;
            for (int i = 0; i < n; i++) {
                int bin = (int) ((metrics[i] - min) / binWidth);
                if (bin > 2) {
                    bin = 2; // guard the max value, which would otherwise fall one bin past the edge
                }
                if (bin <= 0) {
                    states[i] = LoadState.UNDERLOADED;
                    underloaded++;
                } else if (bin == 1) {
                    states[i] = LoadState.BALANCED;
                    balanced++;
                } else {
                    states[i] = LoadState.OVERLOADED;
                    overloaded++;
                }
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] min=", min, " max=", max, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

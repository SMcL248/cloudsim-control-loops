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
 * analyser_v7
 *
 * STRATEGY: Deviation from a fixed efficiency set-point, with asymmetric margins. Level:
 * VM (3). Metric: VM CPU utilisation ratio (0..1).
 *
 * Rationale: this is not a "high vs low" split (v1) or a distribution-relative split
 * (v2/v4/v5/v6/v9). It instead asks "how far is this VM from the utilisation level we
 * actually want it running at?". TARGET_UTIL represents a chosen efficiency sweet spot -
 * VMs well below it are wasting the static power overhead of the guest/host for little
 * work done, VMs well above it are pushing toward saturation. The margins either side of
 * the target are deliberately asymmetric: because idle VMs are a larger, more direct
 * lever on average power than moderately busy ones, the underload margin is narrower
 * (reacts sooner) than the overload margin (more tolerant).
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "vm-loadState-setpointDeviation";

    private static final double TARGET_UTIL = 0.65;
    private static final double UNDERLOAD_MARGIN = 0.25; // triggers below TARGET_UTIL - 0.25 = 0.40
    private static final double OVERLOAD_MARGIN = 0.20;  // triggers above TARGET_UTIL + 0.20 = 0.85

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double overloadTrigger = TARGET_UTIL + OVERLOAD_MARGIN;
        double underloadTrigger = TARGET_UTIL - UNDERLOAD_MARGIN;

        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < n; i++) {
            double util = metrics[i];
            if (util > overloadTrigger) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (util < underloadTrigger) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] setpoint=", TARGET_UTIL, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

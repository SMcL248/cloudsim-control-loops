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
 * analyser_v1
 *
 * STRATEGY: Fixed absolute threshold classification.
 * Level: HOST (2). Metric: host CPU utilisation ratio, expressed on a 0..1 scale,
 * where index i in the input array corresponds to index i of readSpace.getAllHosts().
 *
 * Rationale for power goal: hosts pinned near saturation risk throttled throughput and
 * reactive migrations (both indirectly costly to average power via churn), while hosts
 * sitting far below capacity are prime consolidation/power-down candidates. This variant
 * uses simple, fixed, human-chosen cut points rather than deriving them from the observed
 * cohort - the baseline against which the distribution-relative variants can be compared.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "host-loadState-fixedThreshold";

    private static final double OVERLOAD_THRESHOLD = 0.85;
    private static final double UNDERLOAD_THRESHOLD = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        LoadState[] states = new LoadState[metrics.length];
        int overloaded = 0;
        int underloaded = 0;
        int balanced = 0;

        for (int i = 0; i < metrics.length; i++) {
            double util = metrics[i];
            if (util > OVERLOAD_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (util < UNDERLOAD_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] fixed-threshold host classification complete - overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

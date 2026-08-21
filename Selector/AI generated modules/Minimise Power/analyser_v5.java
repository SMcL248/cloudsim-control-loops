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
 * analyser_v5
 *
 * STRATEGY: Min-max range normalisation. Level: HOST (2). Metric: host power draw in
 * Watts.
 *
 * Rationale: rather than comparing each host to the cohort's average behaviour (v2, v3),
 * this variant compares each host only to the extremes actually observed this call -
 * "where does this host sit between the least and most power-hungry host right now?".
 * A host near the top of the observed range is OVERLOADED, near the bottom UNDERLOADED.
 * This is sensitive to the current spread of the fleet: a fleet that is uniformly busy
 * still gets a full 0..1 spread mapped across it, surfacing relative outliers even without
 * a strong statistical tail, which suits a power goal where every possible consolidation
 * opportunity is worth surfacing.
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-powerDrawWatts";
    private static final String OUTPUT_SEMANTIC = "host-loadState-minMaxNormalised";

    private static final double OVERLOAD_NORMALISED_THRESHOLD = 0.8;
    private static final double UNDERLOAD_NORMALISED_THRESHOLD = 0.2;

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
            // Every host is drawing identical power - nothing stands out from anything else.
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
            }
            balanced = n;
        } else {
            for (int i = 0; i < n; i++) {
                double normalised = (metrics[i] - min) / range;
                if (normalised > OVERLOAD_NORMALISED_THRESHOLD) {
                    states[i] = LoadState.OVERLOADED;
                    overloaded++;
                } else if (normalised < UNDERLOAD_NORMALISED_THRESHOLD) {
                    states[i] = LoadState.UNDERLOADED;
                    underloaded++;
                } else {
                    states[i] = LoadState.BALANCED;
                    balanced++;
                }
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] min=", min, " max=", max, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

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
import java.util.Arrays;

/**
 * analyser_v4
 *
 * STRATEGY: Rank-based quartile buckets. Level: HOST (2). Metric: host CPU utilisation
 * ratio (0..1).
 *
 * Rationale: this variant ignores the magnitude of the metric entirely and only looks at
 * ORDER. The bottom quarter of hosts by rank are UNDERLOADED, the top quarter OVERLOADED,
 * the middle half BALANCED - regardless of how close together or spread out the actual
 * values are. This is deliberately different from the statistical variants (v2, v3, v5):
 * it guarantees a roughly fixed proportion of the fleet is always flagged for
 * consolidation attention, which is a useful property when driving a steady, ongoing
 * power-reduction policy rather than reacting only to statistically unusual spread.
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "host-loadState-rankQuartile";

    private static final double QUARTILE_FRACTION = 0.25;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        // Ascending sort of original indices by their metric value.
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        int lowerCount = (int) Math.floor(n * QUARTILE_FRACTION);
        int upperCount = (int) Math.floor(n * QUARTILE_FRACTION);

        for (int i = 0; i < n; i++) {
            states[i] = LoadState.BALANCED;
        }

        int underloaded = 0;
        int overloaded = 0;

        for (int rank = 0; rank < lowerCount; rank++) {
            states[order[rank]] = LoadState.UNDERLOADED;
            underloaded++;
        }
        for (int rank = n - 1; rank >= n - upperCount; rank--) {
            states[order[rank]] = LoadState.OVERLOADED;
            overloaded++;
        }

        int balanced = n - underloaded - overloaded;

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] rank-quartile classification - n=", n, " overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

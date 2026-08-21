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
 * analyser_v6
 *
 * STRATEGY: Largest-gap ("natural breaks") 1-D clustering. Level: HOST (2). Metric: host
 * CPU utilisation ratio (0..1).
 *
 * Rationale: sort the observed values and look for the two biggest jumps between
 * consecutive values. Those jumps are the most natural place to split the fleet into a
 * low, middle and high group, because they are where the data itself is least crowded -
 * unlike v2/v3/v9 this uses no assumption about a "typical" distribution shape (no mean,
 * no std dev, no percentiles), it only asks where real separation exists in this
 * particular snapshot. When the fleet's utilisation naturally clusters into "idle",
 * "working" and "busy" groups, this variant finds that structure directly.
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final String INPUT_SEMANTIC = "host-cpuUtilRatio-0to1";
    private static final String OUTPUT_SEMANTIC = "host-loadState-naturalBreaks";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];
        if (n == 0) {
            return states;
        }
        if (n < 3) {
            // Not enough points to form three meaningful groups - fall back to a simple
            // ordering: lowest is UNDERLOADED, highest is OVERLOADED, ties are BALANCED.
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
            }
            if (n == 2) {
                if (metrics[0] < metrics[1]) {
                    states[0] = LoadState.UNDERLOADED;
                    states[1] = LoadState.OVERLOADED;
                } else if (metrics[0] > metrics[1]) {
                    states[0] = LoadState.OVERLOADED;
                    states[1] = LoadState.UNDERLOADED;
                }
            }
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] n=", n, " below cluster minimum, used simple fallback ordering");
            return states;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        // Find the two largest gaps between consecutive sorted values - these become the
        // two cluster boundaries (low|mid, mid|high).
        double[] sortedVals = new double[n];
        for (int i = 0; i < n; i++) {
            sortedVals[i] = metrics[order[i]];
        }

        int gap1Index = -1;
        int gap2Index = -1;
        double gap1Size = -1.0;
        double gap2Size = -1.0;
        for (int i = 0; i < n - 1; i++) {
            double gap = sortedVals[i + 1] - sortedVals[i];
            if (gap > gap1Size) {
                gap2Size = gap1Size;
                gap2Index = gap1Index;
                gap1Size = gap;
                gap1Index = i;
            } else if (gap > gap2Size) {
                gap2Size = gap;
                gap2Index = i;
            }
        }

        int lowerBoundary = Math.min(gap1Index, gap2Index == -1 ? gap1Index : gap2Index);
        int upperBoundary = Math.max(gap1Index, gap2Index == -1 ? gap1Index : gap2Index);

        int underloaded = 0;
        int overloaded = 0;
        int balanced = 0;

        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            if (rank <= lowerBoundary) {
                states[originalIndex] = LoadState.UNDERLOADED;
                underloaded++;
            } else if (rank <= upperBoundary) {
                states[originalIndex] = LoadState.BALANCED;
                balanced++;
            } else {
                states[originalIndex] = LoadState.OVERLOADED;
                overloaded++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] natural-break clustering - overloaded=", overloaded, " underloaded=", underloaded, " balanced=", balanced);
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

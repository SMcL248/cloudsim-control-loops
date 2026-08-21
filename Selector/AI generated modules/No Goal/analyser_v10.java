package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

/**
 * analyser_v10 - Cloudlet level, ordinal rank classifier.
 * Strategy: sorts cloudlets by estimated finish time and classifies purely
 * by rank position/count, not by magnitude. The latest-finishing fixed
 * fraction of cloudlets is OVERLOADED and the earliest-finishing fixed
 * fraction is UNDERLOADED, regardless of how close together or far apart
 * their actual finish times are. This is deliberately insensitive to value
 * spacing/outliers, unlike the IQR-fence (v6) or percentile-value (v4)
 * approaches, which are magnitude-sensitive.
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final double RANK_FRACTION = 0.20;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        int cutCount = (int) Math.ceil(n * RANK_FRACTION);

        int overloadCount = 0;
        int underloadCount = 0;

        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            if (rank < cutCount) {
                // earliest finishers - least contended
                result[originalIndex] = LoadState.UNDERLOADED;
                underloadCount++;
            } else if (rank >= n - cutCount) {
                // latest finishers - most contended
                result[originalIndex] = LoadState.OVERLOADED;
                overloadCount++;
            } else {
                result[originalIndex] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] classified ", n,
                " cloudlets by finish-time rank (bottom/top ", RANK_FRACTION,
                " fraction, cutCount=", cutCount, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-estfinishtime-simtime";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadstate-estfinishtime";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}

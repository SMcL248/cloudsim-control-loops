package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

// Strategy: largest-gap natural-break segmentation.
// No mean, no stddev, no fixed percentile - instead this variant sorts the
// batch and looks for the single biggest jump between consecutive values in
// the lower half and in the upper half. Those two biggest jumps are treated
// as the "natural" boundaries the data itself suggests, splitting the fleet
// into three clusters of unequal size rather than forcing a fixed split
// ratio. This tends to find genuine outlier clusters that quantile-based
// splitting (v3) would smear across an arbitrary 25/75 cut.
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v5";

    private static final int INPUT_GUID = 1300;
    private static final int OUTPUT_GUID = 2300;
    private static final String INPUT_SEMANTIC = "vm-throughput-efficiency-ratio";
    private static final String OUTPUT_SEMANTIC = "vm-throughput-natural-break-classification";

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n < 3) {
            for (int i = 0; i < n; i++) {
                result[i] = LoadState.BALANCED;
            }
            return result;
        }

        final double[] values = metrics;
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(values[a], values[b]));

        int mid = n / 2;
        // Largest gap in the lower half separates the low-efficiency
        // (overloaded) cluster from the rest.
        int lowerSplitRank = findLargestGapRank(order, values, 0, mid);
        // Largest gap in the upper half separates the rest from the
        // high-efficiency (underloaded) cluster.
        int upperSplitRank = findLargestGapRank(order, values, mid, n - 1);

        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            if (rank <= lowerSplitRank) {
                result[originalIndex] = LoadState.OVERLOADED;
            } else if (rank > upperSplitRank) {
                result[originalIndex] = LoadState.UNDERLOADED;
            } else {
                result[originalIndex] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME + "] classified ", n,
                " VMs via natural-break segmentation; lowerSplitRank=", lowerSplitRank,
                " upperSplitRank=", upperSplitRank);

        return result;
    }

    // Returns the rank (within [fromRank, toRank]) immediately before the
    // largest gap between consecutive sorted values in that range.
    private int findLargestGapRank(Integer[] order, double[] values, int fromRank, int toRank) {
        int splitRank = fromRank;
        double largestGap = -1.0;

        for (int rank = fromRank; rank < toRank; rank++) {
            double current = values[order[rank]];
            double next = values[order[rank + 1]];
            double gap = next - current;
            if (gap > largestGap) {
                largestGap = gap;
                splitRank = rank;
            }
        }

        return splitRank;
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

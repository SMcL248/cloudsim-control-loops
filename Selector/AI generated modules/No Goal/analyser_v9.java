package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

/**
 * analyser_v9 - Host level, natural-breaks (largest-gap) clustering.
 * Strategy: rather than computing any threshold formula, this variant sorts
 * the observed MIPS headroom fractions and finds the two largest gaps
 * between consecutive sorted values. Those two gaps split the hosts into
 * three contiguous clusters (lowest headroom, middle, highest headroom),
 * which become OVERLOADED, BALANCED and UNDERLOADED respectively. This is a
 * structural/unsupervised approach: it adapts to however the data happens
 * to cluster this cycle, rather than assuming any particular distribution
 * shape or fixed multiplier.
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        if (n < 3) {
            Arrays.fill(result, LoadState.BALANCED);
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] fewer than 3 hosts (", n,
                    "), skipping clustering, all balanced");
            return result;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        // Find the two largest gaps between consecutive sorted values. Each gap is
        // identified by the rank position immediately before it (0..n-2).
        int firstBreak = -1;
        int secondBreak = -1;
        double firstGap = -1.0;
        double secondGap = -1.0;

        for (int i = 0; i < n - 1; i++) {
            double gap = metrics[order[i + 1]] - metrics[order[i]];
            if (gap > firstGap) {
                secondGap = firstGap;
                secondBreak = firstBreak;
                firstGap = gap;
                firstBreak = i;
            } else if (gap > secondGap) {
                secondGap = gap;
                secondBreak = i;
            }
        }

        int lowBreak = Math.min(firstBreak, secondBreak);
        int highBreak = Math.max(firstBreak, secondBreak);

        int overloadCount = 0;
        int underloadCount = 0;

        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            if (rank <= lowBreak) {
                result[originalIndex] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (rank > highBreak) {
                result[originalIndex] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[originalIndex] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] classified ", n,
                " hosts by natural-breaks clustering -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-mipsheadroom-fraction";
    }

    @Override
    public String outputSemantic() {
        return "host-loadstate-mipsheadroom";
    }

    @Override
    public int inputGuid() {
        return 1200;
    }

    @Override
    public int outputGuid() {
        return 2200;
    }
}

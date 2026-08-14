package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 9 - Cloudlet-level PE demand, percentile-rank
 * classification.
 *
 * Strategy: ranks each cloudlet's PE (processing element) demand against
 * the full population observed this cycle and classifies purely on rank
 * (top 20% / bottom 20% / middle 60%), rather than on the magnitude of the
 * value itself. This is a non-parametric approach: it makes no assumption
 * about the shape of the distribution (normal, skewed, multi-modal, etc.)
 * and is insensitive to a handful of extreme outliers, unlike a mean/std
 * or min/max based boundary.
 *
 * Goal alignment: mixed. Cloudlets demanding the most PEs relative to
 * their peers are flagged OVERLOADED - they are the biggest throughput
 * risk if starved of PEs, and the most expensive to host in power terms;
 * the lightest-demand cloudlets are flagged UNDERLOADED as cheap to place
 * or consolidate.
 *
 * Level: cloudlet (level 4). Input/output arrays are positionally aligned
 * with readSpace.getActiveCloudlets().
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    private static final double LOW_RANK_FRACTION = 0.20;
    private static final double HIGH_RANK_FRACTION = 0.80;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Double.compare(metrics[a], metrics[b]));

        double[] rankFraction = new double[n];
        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order[rank];
            rankFraction[originalIndex] = (n > 1) ? ((double) rank / (n - 1)) : 0.5;
        }

        for (int i = 0; i < n; i++) {
            if (rankFraction[i] >= HIGH_RANK_FRACTION) {
                states[i] = LoadState.OVERLOADED;
            } else if (rankFraction[i] <= LOW_RANK_FRACTION) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v9] classified ", n,
                " cloudlets by pe-demand percentile rank, lowCut=", LOW_RANK_FRACTION,
                " highCut=", HIGH_RANK_FRACTION);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-peDemand: readSpace.getCloudletNumberOfPes for the cloudlet, count of processing elements requested, one entry per cloudlet in readSpace.getActiveCloudlets() order";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadState: OVERLOADED if peDemand ranks at or above the 80th percentile of this cycle's cloudlet population, UNDERLOADED if at or below the 20th percentile, else BALANCED";
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

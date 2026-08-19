package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Variant: analyser_v5
 * Level: HOST
 * Metric: available MIPS headroom
 * Strategy: rank-based (percentile position) classification. Unlike a
 * value-based fence, this looks only at each host's ORDINAL position within
 * the sorted batch, so it is insensitive to the absolute spread of values -
 * it always flags the bottom quarter and top quarter of the batch, however
 * tightly or widely the values happen to be clustered this cycle.
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double LOW_RANK_CUTOFF = 0.25;
    private static final double HIGH_RANK_CUTOFF = 0.75;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(idx -> metrics[idx]));

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order.get(rank);
            double fractionalRank = n <= 1 ? 0.5 : (double) rank / (n - 1);
            LoadState state;
            if (fractionalRank <= LOW_RANK_CUTOFF) {
                state = LoadState.OVERLOADED; // lowest headroom -> most loaded
            } else if (fractionalRank >= HIGH_RANK_CUTOFF) {
                state = LoadState.UNDERLOADED; // highest headroom -> spare capacity
            } else {
                state = LoadState.BALANCED;
            }
            states[originalIndex] = state;
            switch (state) {
                case OVERLOADED: overloaded++; break;
                case UNDERLOADED: underloaded++; break;
                default: balanced++; break;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v5] headroom percentile-rank classification: ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-available-mips-headroom";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-percentile-rank";
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

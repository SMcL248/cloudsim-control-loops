package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/*
 * Variant: analyser_v7
 * Level: VM
 * Metric: effective throughput (MIPS)
 * Strategy: two-stage classification. Stage 1 pulls out VMs doing
 * essentially no work (throughput ~ 0) and labels them UNDERLOADED directly
 * - idle VMs should not be ranked against active ones. Stage 2 ranks the
 * remaining, genuinely active VMs by decile: the busiest 10% are
 * OVERLOADED, the quietest 10% of the active group are UNDERLOADED, and the
 * rest are BALANCED.
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final double ZERO_EPSILON = 1e-6;
    private static final double LOW_DECILE = 0.10;
    private static final double HIGH_DECILE = 0.90;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        List<Integer> activeIndices = new ArrayList<>();
        int idleCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] <= ZERO_EPSILON) {
                states[i] = LoadState.UNDERLOADED;
                idleCount++;
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] vm ", readSpace.getId(vms.get(i)), " throughput~0 -> UNDERLOADED (idle)");
            } else {
                activeIndices.add(i);
            }
        }

        activeIndices.sort(Comparator.comparingDouble(idx -> metrics[idx]));
        int activeN = activeIndices.size();
        int overloaded = 0, underloaded = 0, balanced = 0;

        for (int rank = 0; rank < activeN; rank++) {
            int originalIndex = activeIndices.get(rank);
            double fractionalRank = activeN <= 1 ? 0.5 : (double) rank / (activeN - 1);
            LoadState state;
            if (fractionalRank >= HIGH_DECILE) {
                state = LoadState.OVERLOADED;
            } else if (fractionalRank <= LOW_DECILE) {
                state = LoadState.UNDERLOADED;
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

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v7] zero-plus-decile classification: ", idleCount, " idle, ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-effective-throughput-mips";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-zero-decile-rank";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}

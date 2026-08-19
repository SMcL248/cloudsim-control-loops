package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 * Variant: analyser_v9
 * Level: VM
 * Metric: CPU utilisation fraction
 * Strategy: natural-breaks (largest-gap) clustering, a purely data-driven
 * alternative to moment-based (mean/std) or order-based (percentile)
 * boundaries. The VMs are sorted by utilisation and the two biggest gaps
 * between consecutive sorted values are used as the cluster boundaries,
 * splitting the batch into whatever three groups the data itself naturally
 * falls into this cycle - there is no fixed target proportion for how many
 * VMs end up in each state.
 */
public class analyser_v9 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        if (n == 0) {
            return states;
        }
        if (n < 3) {
            // Not enough points to form three meaningful clusters; treat
            // everything as BALANCED rather than guessing.
            Arrays.fill(states, LoadState.BALANCED);
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] fewer than 3 VMs, defaulting to BALANCED");
            return states;
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            order.add(i);
        }
        order.sort(Comparator.comparingDouble(idx -> metrics[idx]));

        // Find the two largest gaps between consecutive sorted values.
        int firstBreak = -1;
        int secondBreak = -1;
        double firstGap = -1.0;
        double secondGap = -1.0;
        for (int i = 1; i < n; i++) {
            double gap = metrics[order.get(i)] - metrics[order.get(i - 1)];
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

        int otherBreak = secondBreak < 0 ? firstBreak : secondBreak;
        int lowBreak = Math.min(firstBreak, otherBreak);
        int highBreak = Math.max(firstBreak, otherBreak);

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int rank = 0; rank < n; rank++) {
            int originalIndex = order.get(rank);
            LoadState state;
            if (rank < lowBreak) {
                state = LoadState.UNDERLOADED;
            } else if (rank >= highBreak) {
                state = LoadState.OVERLOADED;
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

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v9] natural-breaks classification: ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-fraction";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-natural-breaks";
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

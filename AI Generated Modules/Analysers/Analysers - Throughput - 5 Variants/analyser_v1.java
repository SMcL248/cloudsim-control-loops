package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v1 - VM Cloudlet Count, Tertile Rank Classification
 *
 * Classifies each VM by the number of cloudlets currently allocated to it.
 * Rather than a fixed or mean-relative cutoff, this variant ranks all VMs
 * in the snapshot by cloudlet count and splits the ranked list into three
 * equal-sized bands:
 *
 *   bottom third (by rank) -> UNDERLOADED
 *   top third    (by rank) -> OVERLOADED
 *   middle third           -> BALANCED
 *
 * Rank-based bands always produce a fixed proportion of each state
 * regardless of the absolute scale of the counts, which is useful when
 * comparing snapshots across very different workload sizes. If every VM
 * carries an identical count, ranking is meaningless and all VMs are
 * reported BALANCED.
 *
 * inputGuid  : vm-count
 * outputGuid : vm-count-loadstate
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : metrics) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        if (n == 0 || max == min) {
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
                Log.printlnConcat(now, ": [analyser_v1] VM ", i,
                        " cloudlet-count=", metrics[i],
                        " state=BALANCED (no rank separation)");
            }
            return states;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> metrics[i]));

        int lowerCut = n / 3;
        int upperCut = n - n / 3;

        for (int rank = 0; rank < n; rank++) {
            int vmIndex = order[rank];
            LoadState state;
            if (rank < lowerCut) {
                state = LoadState.UNDERLOADED;
            } else if (rank >= upperCut) {
                state = LoadState.OVERLOADED;
            } else {
                state = LoadState.BALANCED;
            }
            states[vmIndex] = state;

            Log.printlnConcat(now, ": [analyser_v1] VM ", vmIndex,
                    " cloudlet-count=", metrics[vmIndex],
                    " rank=", rank, "/", n, " state=", state);
        }

        return states;
    }

    @Override
    public String inputGuid() {
        return "vm-count";
    }

    @Override
    public String outputGuid() {
        return "vm-count-loadstate";
    }
}

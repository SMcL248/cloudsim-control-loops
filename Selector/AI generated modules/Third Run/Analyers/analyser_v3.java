package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 3 - Host-level free-MIPS fraction, data-driven quartile
 * band.
 *
 * Strategy: classifies each host's spare-capacity fraction (free MIPS /
 * total MIPS) against the 25th and 75th percentiles computed fresh from
 * this cycle's fleet snapshot. Low spare capacity relative to the fleet
 * this cycle means OVERLOADED; high spare capacity means UNDERLOADED.
 * Quartiles are recomputed every call, so the boundary tracks whatever the
 * fleet's spread looks like right now rather than an assumed distribution.
 *
 * Goal alignment: throughput-leaning. Hosts with little spare capacity are
 * flagged before an incoming cloudlet/VM would be starved of MIPS; hosts
 * with abundant spare capacity are flagged as targets to receive migrated
 * work, freeing up sibling hosts for power-down.
 *
 * Level: host (level 2). Input/output arrays are positionally aligned with
 * readSpace.getAllHosts().
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double q1 = percentile(metrics, 25.0);
        double q3 = percentile(metrics, 75.0);

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v < q1) {
                states[i] = LoadState.OVERLOADED;
            } else if (v > q3) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v3] classified ", n,
                " hosts by free mips fraction, q1=", q1, " q3=", q3);

        return states;
    }

    private double percentile(double[] values, double pct) {
        int n = values.length;
        if (n == 0) {
            return 0.0;
        }
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);
        if (n == 1) {
            return sorted[0];
        }
        double rank = (pct / 100.0) * (n - 1);
        int lowIndex = (int) Math.floor(rank);
        int highIndex = (int) Math.ceil(rank);
        if (lowIndex == highIndex) {
            return sorted[lowIndex];
        }
        double weight = rank - lowIndex;
        return sorted[lowIndex] + weight * (sorted[highIndex] - sorted[lowIndex]);
    }

    @Override
    public String inputSemantic() {
        return "host-freeMipsFraction: fraction of host total MIPS currently unallocated (availableMips / totalMips), range 0..1, one entry per host in readSpace.getAllHosts() order";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState: OVERLOADED if freeMipsFraction is below this cycle's fleet 25th percentile, UNDERLOADED if above the 75th percentile, else BALANCED";
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

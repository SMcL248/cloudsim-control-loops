package org.cloudbus.cloudsim.examples;

import java.util.Arrays;

import org.cloudbus.cloudsim.Log;

/**
 * Analyser variant 7 - VM-level MIPS headroom, IQR band with a hysteresis
 * margin.
 *
 * Strategy: classifies each VM's MIPS headroom (requested MIPS minus
 * currently allocated MIPS - how much more the VM is asking for than it is
 * getting) against an interquartile-range band. The band is not the raw
 * [Q1, Q3] edges but is padded outward by a hysteresis margin (10% of the
 * IQR) on both sides, so values sitting just outside the quartiles are
 * still treated as BALANCED. This absorbs cycle-to-cycle measurement noise
 * that would otherwise cause a VM to flap between states every control
 * cycle.
 *
 * Goal alignment: mixed. Large positive headroom (VM wants much more than
 * it has) risks throughput loss and is flagged OVERLOADED; near-zero or
 * negative headroom (VM is allocated more than it is asking for) is a
 * power/consolidation opportunity and is flagged UNDERLOADED.
 *
 * Level: VM (level 3). Input/output arrays are positionally aligned with
 * readSpace.getVmList().
 */
public class analyser_v7 implements Analyser<double[], LoadState[]> {

    private static final double HYSTERESIS_FRACTION = 0.10;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double q1 = percentile(metrics, 25.0);
        double q3 = percentile(metrics, 75.0);
        double iqr = q3 - q1;
        double margin = iqr * HYSTERESIS_FRACTION;

        double upperBound = q3 + margin;
        double lowerBound = q1 - margin;

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (v < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v7] classified ", n,
                " vms by mips headroom, q1=", q1, " q3=", q3,
                " hysteresisMargin=", margin);

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
        return "vm-mipsHeadroom: readSpace.getVmRequestedMips minus readSpace.getVmMips for the VM, signed MIPS, one entry per VM in readSpace.getVmList() order";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadState: OVERLOADED if mipsHeadroom is above this cycle's Q3 plus a 10%-of-IQR hysteresis margin, UNDERLOADED if below Q1 minus the same margin, else BALANCED";
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

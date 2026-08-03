package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v2 - Host-level power draw classifier (interquartile range).
 *
 * Level        : Host (level 2)
 * Metric       : Per-host instantaneous power draw (Watts).
 * Threshold    : Dynamic - quartiles Q1/Q3 are computed from the observed
 *                power readings each cycle and used to build an IQR-based
 *                mild-outlier band. Hosts drawing power above
 *                Q3 + 0.5*IQR are OVERLOADED (consolidation candidates
 *                driving power up); hosts below Q1 - 0.5*IQR are
 *                UNDERLOADED (idle-ish, shutdown candidates) - directly
 *                supports the minimise-power goal.
 * Power rule   : A host already powered down is always UNDERLOADED,
 *                since it is drawing no meaningful load-related power.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double[] sorted = Arrays.copyOf(metrics, n);
        Arrays.sort(sorted);

        double q1 = percentile(sorted, 0.25);
        double q3 = percentile(sorted, 0.75);
        double iqr = q3 - q1;

        double upperBound = q3 + 0.5 * iqr;
        double lowerBound = q1 - 0.5 * iqr;

        for (int i = 0; i < n; i++) {
            boolean poweredDown = (i < hosts.size()) && readSpace.isHostPoweredDown(hosts.get(i));

            if (poweredDown) {
                states[i] = LoadState.UNDERLOADED;
            } else if (metrics[i] > upperBound) {
                states[i] = LoadState.OVERLOADED;
            } else if (metrics[i] < lowerBound) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v2] classified ", n,
                " hosts, q1=", q1, " q3=", q3, " iqr=", iqr);

        return states;
    }

    private double percentile(double[] sortedValues, double fraction) {
        int n = sortedValues.length;
        if (n == 0) {
            return 0.0;
        }
        if (n == 1) {
            return sortedValues[0];
        }
        double pos = fraction * (n - 1);
        int lowIdx = (int) Math.floor(pos);
        int highIdx = (int) Math.ceil(pos);
        if (lowIdx == highIdx) {
            return sortedValues[lowIdx];
        }
        double weight = pos - lowIdx;
        return sortedValues[lowIdx] * (1.0 - weight) + sortedValues[highIdx] * weight;
    }

    @Override
    public String inputSemantic() {
        return "host-power-draw-watts";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-iqr";
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

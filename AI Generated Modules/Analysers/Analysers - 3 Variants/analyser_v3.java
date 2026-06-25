package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v3 — VM Count, Statistical Relative Analysis.
 *
 * Input metric : number of VMs currently deployed on each host (host-vm-count)
 * Output       : LoadState per host                           (host-loadstate)
 *
 * Instead of fixed thresholds this analyser evaluates each host relative to
 * the cluster-wide distribution computed from the same metrics snapshot:
 *
 *   mean   = average VM count across all hosts
 *   stddev = population standard deviation of VM counts
 *   upper  = mean + DEVIATION_FACTOR * stddev
 *   lower  = mean - DEVIATION_FACTOR * stddev  (clamped to MIN_VMS)
 *
 * Classification:
 *   count > upper  (and stddev > 0) -> OVERLOADED   (significantly above average)
 *   count < lower  (and stddev > 0) -> UNDERLOADED  (significantly below average)
 *   count < MIN_VMS                 -> UNDERLOADED  (absolute floor: near-empty host)
 *   otherwise                       -> BALANCED
 *
 * When stddev == 0 all hosts carry equal load; only the absolute MIN_VMS
 * floor can still trigger UNDERLOADED, preventing false positives on a
 * perfectly balanced cluster.
 *
 * The actionable-cycle counter increments once per analyse() call in which
 * at least one host is OVERLOADED or UNDERLOADED.
 */
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    /** Multiplier applied to stddev to define the overload/underload bands. */
    private static final double DEVIATION_FACTOR = 1.0;

    /** Hosts with fewer than this many VMs are always UNDERLOADED. */
    private static final double MIN_VMS = 1.0;

    private int actionableCycles = 0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];
        boolean actionable = false;

        if (metrics.length == 0) {
            return states;
        }

        // --- Compute population mean ---
        double sum = 0.0;
        for (double v : metrics) sum += v;
        double mean = sum / metrics.length;

        // --- Compute population standard deviation ---
        double variance = 0.0;
        for (double v : metrics) variance += (v - mean) * (v - mean);
        double stddev = Math.sqrt(variance / metrics.length);

        double upper = mean + DEVIATION_FACTOR * stddev;
        double lower = Math.max(mean - DEVIATION_FACTOR * stddev, MIN_VMS);

        Log.printlnConcat(now, ": [analyser_v3] vm-count stats: mean=",
                          String.format("%.2f", mean), " stddev=", String.format("%.2f", stddev),
                          " upper=", String.format("%.2f", upper),
                          " lower=", String.format("%.2f", lower));

        // --- Classify each host ---
        for (int i = 0; i < metrics.length; i++) {
            double count = metrics[i];

            if (count < MIN_VMS) {
                // Absolute floor: host is near-empty regardless of cluster distribution
                states[i] = LoadState.UNDERLOADED;
                actionable = true;
            } else if (stddev > 0 && count > upper) {
                states[i] = LoadState.OVERLOADED;
                actionable = true;
            } else if (stddev > 0 && count < lower) {
                states[i] = LoadState.UNDERLOADED;
                actionable = true;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v3] Host[", i, "] vm-count=",
                              (int) count, " -> ", states[i]);
        }

        if (actionable) {
            actionableCycles++;
            Log.printlnConcat(now, ": [analyser_v3] Actionable cycle detected (total=", actionableCycles, ")");
        }

        return states;
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }

    @Override
    public String inputGuid() {
        return "host-vm-count";
    }

    @Override
    public String outputGuid() {
        return "host-vm-count-loadstate";
    }
}

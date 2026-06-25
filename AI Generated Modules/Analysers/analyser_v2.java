package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v2 — CPU Demand Pressure, Saturation-Aware Analysis.
 *
 * Input metric : host CPU demand pressure (requested MIPS / total MIPS).
 *                Values may exceed 1.0 when demand cannot be fully satisfied.
 *                (host-cpu-demand)
 * Output       : LoadState per host  (host-loadstate)
 *
 * Classification logic:
 *   demand >= SATURATION_THRESHOLD  -> OVERLOADED
 *       Covers both the saturation zone (0.9–1.0) and unsatisfiable demand (> 1.0).
 *       A host approaching or past its MIPS ceiling cannot reliably absorb more work.
 *   demand <  IDLE_THRESHOLD        -> UNDERLOADED
 *       Host is largely idle and a candidate for VM consolidation.
 *   otherwise                       -> BALANCED
 *
 * The actionable-cycle counter increments once per analyse() call in which
 * at least one host is OVERLOADED or UNDERLOADED.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    /** Demand ratio at or above which a host is considered overloaded. */
    private static final double SATURATION_THRESHOLD = 0.90;

    /** Demand ratio below which a host is considered underloaded. */
    private static final double IDLE_THRESHOLD = 0.15;

    private int actionableCycles = 0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];
        boolean actionable = false;

        for (int i = 0; i < metrics.length; i++) {
            double demand = metrics[i];

            if (demand >= SATURATION_THRESHOLD) {
                // Includes demand > 1.0 (unsatisfiable) automatically
                states[i] = LoadState.OVERLOADED;
                actionable = true;
            } else if (demand < IDLE_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
                actionable = true;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v2] Host[", i, "] cpu-demand=",
                              String.format("%.4f", demand), " -> ", states[i]);
        }

        if (actionable) {
            actionableCycles++;
            Log.printlnConcat(now, ": [analyser_v2] Actionable cycle detected (total=", actionableCycles, ")");
        }

        return states;
    }

    @Override
    public int getActionableCycles() {
        return actionableCycles;
    }

    @Override
    public String inputGuid() {
        return "host-cpu-demand";
    }

    @Override
    public String outputGuid() {
        return "host-loadstate";
    }
}

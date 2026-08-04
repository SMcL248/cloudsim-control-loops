package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;

// Host-level analyser. Interprets metrics[i] as a per-host power-draw
// ratio (current power / max power, in [0,1]) aligned with
// readSpace.getAllHosts(). Uses fixed physical thresholds rather than
// distribution-derived ones, since the ratio is already normalised to a
// meaningful [0,1] scale: hosts drawing near-peak power are flagged
// OVERLOADED (energy-inefficient / consolidation risk), hosts drawing
// near-idle power are flagged UNDERLOADED (power-down candidates).
public class analyser_v3 implements Analyser<double[], LoadState[]> {

    private static final String MODULE_NAME = "analyser_v3";
    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double HIGH_POWER_THRESHOLD = 0.85;
    private static final double LOW_POWER_THRESHOLD = 0.15;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        for (int i = 0; i < n; i++) {
            double v = metrics[i];
            if (v >= HIGH_POWER_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
            } else if (v <= LOW_POWER_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [" + MODULE_NAME
                + "] classified " + n + " hosts via power-draw-ratio fixed thresholds (high="
                + HIGH_POWER_THRESHOLD + ", low=" + LOW_POWER_THRESHOLD + ")");

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-power-draw-ratio";
    }

    @Override
    public String outputSemantic() {
        return "host-load-classification-power-fixed-ratio";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}

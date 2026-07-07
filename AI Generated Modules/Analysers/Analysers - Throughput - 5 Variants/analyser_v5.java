package org.cloudbus.cloudsim.examples;

import java.util.Arrays;
import java.util.List;

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v5 - VM Estimated Finish-Time, Exponential Moving Average
 *
 * Classifies each VM by its maximum estimated finish-time duration
 * (getEstimatedFinishTime() - time). Rather than classifying the raw
 * snapshot value directly, this variant maintains an exponential moving
 * average (EMA) per VM index across successive control loop invocations
 * and classifies the smoothed value against fixed thresholds:
 *
 *   ema[i] = ALPHA * duration + (1 - ALPHA) * ema[i]
 *   OVERLOADED  : ema > 500.0
 *   UNDERLOADED : ema < 20.0
 *   BALANCED    : otherwise
 *
 * Smoothing damps single-cycle noise spikes so a VM does not flip states
 * on one transient reading. History is reset if the snapshot size changes
 * between calls (e.g. VMs created/destroyed).
 *
 * inputGuid  : vm-etc
 * outputGuid : vm-etc-loadstate
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double ALPHA = 0.30;
    private static final double OVER_THRESHOLD  = 500.0;
    private static final double UNDER_THRESHOLD = 20.0;

    private static double[] ema = new double[0];

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        if (ema.length != n) {
            ema = Arrays.copyOf(metrics, n);
        }

        for (int i = 0; i < n; i++) {
            ema[i] = ALPHA * metrics[i] + (1.0 - ALPHA) * ema[i];

            if (ema[i] > OVER_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
            } else if (ema[i] < UNDER_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v5] VM ", i,
                    " etc=", metrics[i], " ema=", ema[i], " state=", states[i]);
        }

        return states;
    }

    @Override
    public String inputGuid() {
        return "vm-etc";
    }

    @Override
    public String outputGuid() {
        return "vm-etc-loadstate";
    }
}

package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * analyser_v5 — VM Count, Mean-Relative Thresholds
 *
 * Classifies each host by the number of VMs it currently hosts.
 * Thresholds are expressed as multiples of the mean VM count across all hosts:
 *
 *   OVERLOADED  : count > mean * 1.5  (significantly more VMs than average)
 *   UNDERLOADED : count < mean * 0.5  (significantly fewer VMs than average)
 *   BALANCED    : otherwise
 *
 * When the mean is 0 (no VMs deployed anywhere) all hosts are BALANCED.
 * This policy targets consolidation and spread imbalance rather than raw
 * computational load, complementing CPU-based analysers.
 *
 * inputGuid  : host-vm-count
 * outputGuid : host-vm-count-loadstate
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double HIGH_RATIO = 1.5;
    private static final double LOW_RATIO  = 0.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        // Compute mean VM count across all hosts
        double sum = 0;
        for (double v : metrics) sum += v;
        double mean = (metrics.length > 0) ? sum / metrics.length : 0.0;

        double overThreshold  = mean * HIGH_RATIO;
        double underThreshold = mean * LOW_RATIO;

        for (int i = 0; i < metrics.length; i++) {
            double count = metrics[i];
            if (mean == 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (count > overThreshold) {
                states[i] = LoadState.OVERLOADED;
            } else if (count < underThreshold) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v5] Host ", i,
                    " vm-count=", count, " mean=", mean,
                    " thresholds=[", underThreshold, ",", overThreshold, "]",
                    " state=", states[i]);
        }

        return states;
    }

    @Override
    public String inputGuid()  { return "host-vm-count"; }

    @Override
    public String outputGuid() { return "host-vm-count-loadstate"; }
}

package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * analyser_v2 — CPU Utilisation, Adaptive Mean +/- StdDev Thresholds
 *
 * Classifies each host by its CPU utilisation (used MIPS / total MIPS).
 * Thresholds adapt to the current snapshot: a host is OVERLOADED if its
 * utilisation exceeds (mean + stddev), and UNDERLOADED if below (mean - stddev).
 * When all hosts are uniform (stddev = 0) every host is BALANCED.
 *
 * This approach responds to relative imbalance rather than absolute load,
 * making it robust across heterogeneous or lightly-loaded datacenters.
 *
 * inputGuid  : host-cpu-util
 * outputGuid : host-cpu-util-loadstate
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        LoadState[] states = new LoadState[metrics.length];

        // Compute mean utilisation
        double sum = 0;
        for (double v : metrics) sum += v;
        double mean = (metrics.length > 0) ? sum / metrics.length : 0.0;

        // Compute population standard deviation
        double variance = 0;
        for (double v : metrics) variance += (v - mean) * (v - mean);
        double stddev = (metrics.length > 1) ? Math.sqrt(variance / metrics.length) : 0.0;

        double overThreshold  = mean + stddev;
        double underThreshold = mean - stddev;


        for (int i = 0; i < metrics.length; i++) {
            double util = metrics[i];
            if (stddev == 0.0) {
                states[i] = LoadState.BALANCED;
            } else if (util > overThreshold) {
                states[i] = LoadState.OVERLOADED;
            } else if (util < underThreshold) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v2] Host ", i,
                    " cpu-util=", util, " mean=", mean, " stddev=", stddev,
                    " state=", states[i]);
        }

        return states;
    }


    @Override
    public String inputGuid()  { return "host-cpu-util"; }

    @Override
    public String outputGuid() { return "host-cpu-util-loadstate"; }
}

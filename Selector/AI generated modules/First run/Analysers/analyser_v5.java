package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

/**
 * Analyser v5 - Host-level available bandwidth classifier (min-max
 * normalised tertiles).
 *
 * Level        : Host (level 2)
 * Metric       : Per-host available bandwidth as a fraction of total
 *                bandwidth, range [0, 1].
 * Threshold    : Dynamic - each reading is min-max normalised against
 *                the observed range for the current snapshot, then
 *                bucketed into thirds. The bottom third (least relative
 *                headroom) is OVERLOADED, the top third (most relative
 *                headroom) is UNDERLOADED.
 * Death rule   : A permanently dead host is always UNDERLOADED - it
 *                holds no useful bandwidth headroom and should not be
 *                considered for further allocation.
 */
public class analyser_v5 implements Analyser<double[], LoadState[]> {

    private static final double LOWER_TERTILE = 1.0 / 3.0;
    private static final double UPPER_TERTILE = 2.0 / 3.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<HostEntity> hosts = readSpace.getAllHosts();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : metrics) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double range = max - min;

        for (int i = 0; i < n; i++) {
            boolean dead = (i < hosts.size()) && readSpace.isHostPermanentlyDead(hosts.get(i));

            if (dead) {
                states[i] = LoadState.UNDERLOADED;
                continue;
            }

            double normalised = (range > 0.0) ? (metrics[i] - min) / range : 0.5;

            if (normalised < LOWER_TERTILE) {
                states[i] = LoadState.OVERLOADED;
            } else if (normalised > UPPER_TERTILE) {
                states[i] = LoadState.UNDERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(now, ": [analyser_v5] classified ", n,
                " hosts, bw-range-min=", min, " bw-range-max=", max);

        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-bw-available-ratio";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-minmax-tertile";
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

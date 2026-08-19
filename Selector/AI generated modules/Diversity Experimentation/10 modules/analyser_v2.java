package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.List;

/*
 * Variant: analyser_v2
 * Level: HOST
 * Metric: power consumption (watts)
 * Strategy: population z-score over host power draw. Powered-down hosts are
 * excluded from the mean/std-dev computation (their near-zero draw would
 * otherwise drag the whole distribution down and mask genuine outliers among
 * the active hosts), but are still labelled UNDERLOADED directly since they
 * are doing no work at all.
 */
public class analyser_v2 implements Analyser<double[], LoadState[]> {

    private static final double Z_THRESHOLD = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        List<HostEntity> hosts = readSpace.getAllHosts();
        LoadState[] states = new LoadState[metrics.length];

        boolean[] active = new boolean[metrics.length];
        double sum = 0.0;
        int activeCount = 0;

        for (int i = 0; i < metrics.length; i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostPoweredDown(host)) {
                active[i] = false;
                states[i] = LoadState.UNDERLOADED;
                Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] host ", readSpace.getId(host), " is powered down -> UNDERLOADED");
            } else {
                active[i] = true;
                sum += metrics[i];
                activeCount++;
            }
        }

        if (activeCount == 0) {
            Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] no active hosts to score");
            return states;
        }

        double mean = sum / activeCount;
        double sqDiff = 0.0;
        for (int i = 0; i < metrics.length; i++) {
            if (active[i]) {
                double d = metrics[i] - mean;
                sqDiff += d * d;
            }
        }
        double std = Math.sqrt(sqDiff / activeCount);

        int overloaded = 0, underloaded = 0, balanced = 0;
        for (int i = 0; i < metrics.length; i++) {
            if (!active[i]) {
                continue;
            }
            if (std < 1e-9) {
                states[i] = LoadState.BALANCED;
                balanced++;
                continue;
            }
            double z = (metrics[i] - mean) / std;
            if (z >= Z_THRESHOLD) {
                states[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (z <= -Z_THRESHOLD) {
                states[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                states[i] = LoadState.BALANCED;
                balanced++;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v2] power z-score classification: mean=", mean, " std=", std, " -> ", overloaded, " overloaded, ", underloaded, " underloaded, ", balanced, " balanced");
        return states;
    }

    @Override
    public String inputSemantic() {
        return "host-power-consumption-watts";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-power-zscore";
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

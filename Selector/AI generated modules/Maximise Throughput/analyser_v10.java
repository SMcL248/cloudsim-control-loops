package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Variant 10 - HOST level - Failure-aware filtered statistics.
 * Assumes the input metric is per-host instantaneous CPU utilisation
 * as a fraction in [0,1] (same physical metric as variant 1). Before
 * doing any statistics, this analyser partitions hosts using
 * readSpace's failure/power-state queries: failed or permanently dead
 * hosts are forced to OVERLOADED (they cannot be trusted to accept or
 * continue work), intentionally powered-down hosts are forced to
 * UNDERLOADED (they are deliberately idle and are consolidation
 * candidates, not a fault). The mean and standard deviation used to
 * classify the remaining healthy hosts are computed only over that
 * healthy subset, so a handful of failed hosts sitting at 0
 * utilisation cannot distort the reference point for everyone else.
 */
public class analyser_v10 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double Z_THRESHOLD = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        int limit = Math.min(n, hosts.size());

        boolean[] forced = new boolean[n];

        double sum = 0.0;
        int healthyCount = 0;

        for (int i = 0; i < limit; i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host)) {
                result[i] = LoadState.OVERLOADED;
                forced[i] = true;
            } else if (readSpace.isHostPoweredDown(host)) {
                result[i] = LoadState.UNDERLOADED;
                forced[i] = true;
            } else {
                sum += metrics[i];
                healthyCount++;
            }
        }
        // Any indices beyond the host list (defensive) are treated as healthy.
        for (int i = limit; i < n; i++) {
            sum += metrics[i];
            healthyCount++;
        }

        double mean = healthyCount > 0 ? sum / healthyCount : 0.0;

        double sqDiffSum = 0.0;
        for (int i = 0; i < n; i++) {
            if (!forced[i]) {
                sqDiffSum += (metrics[i] - mean) * (metrics[i] - mean);
            }
        }
        double std = healthyCount > 0 ? Math.sqrt(sqDiffSum / healthyCount) : 0.0;

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            if (forced[i]) {
                if (result[i] == LoadState.OVERLOADED) overloaded++;
                if (result[i] == LoadState.UNDERLOADED) underloaded++;
                continue;
            }
            double z = std > 0.0 ? (metrics[i] - mean) / std : 0.0;
            if (z > Z_THRESHOLD) {
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (z < -Z_THRESHOLD) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v10] classified ", n,
            " hosts (", healthyCount, " healthy) via failure-filtered z-score (mean=",
            mean, ", std=", std, ") -> overloaded=", overloaded, ", underloaded=",
            underloaded, ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-cpuUtilFraction-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState-failureFilteredZscore";
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

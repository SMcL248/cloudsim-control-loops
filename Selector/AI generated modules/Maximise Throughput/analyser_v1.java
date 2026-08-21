package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.List;

/**
 * Variant 1 - HOST level - Z-score classification.
 * Assumes the input metric is per-host instantaneous CPU utilisation
 * as a fraction in [0,1]. Classifies each host relative to the
 * population mean and standard deviation of the observed snapshot.
 */
public class analyser_v1 implements Analyser<double[], LoadState[]> {

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

        double sum = 0.0;
        for (double v : metrics) {
            sum += v;
        }
        double mean = sum / n;

        double sqDiffSum = 0.0;
        for (double v : metrics) {
            sqDiffSum += (v - mean) * (v - mean);
        }
        double std = Math.sqrt(sqDiffSum / n);

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
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

        List<HostEntity> hosts = readSpace.getAllHosts();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v1] classified ", n,
            " of ", hosts.size(), " hosts via z-score (mean=", mean, ", std=", std,
            ") -> overloaded=", overloaded, ", underloaded=", underloaded,
            ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-cpuUtilFraction-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState-zscoreMeanStd";
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

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Variant 4 - HOST level - Median Absolute Deviation classification.
 * Assumes the input metric is per-host MIPS headroom fraction
 * (availableMips / totalMips), in [0,1], where higher values mean
 * more idle capacity. Uses the median and the scaled median absolute
 * deviation (MAD) instead of mean/standard deviation, making the
 * classification robust to a handful of extreme outlier hosts (e.g.
 * a saturated or idle host does not drag the reference point).
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int INPUT_GUID = 1200;
    private static final int OUTPUT_GUID = 2200;
    private static final double MAD_SCALE = 1.4826; // consistency constant for normal dist
    private static final double K = 1.0;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double median = median(metrics);

        double[] absDevs = new double[n];
        for (int i = 0; i < n; i++) {
            absDevs[i] = Math.abs(metrics[i] - median);
        }
        double mad = median(absDevs) * MAD_SCALE;

        int overloaded = 0;
        int underloaded = 0;

        for (int i = 0; i < n; i++) {
            if (mad <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            double headroom = metrics[i];
            if (headroom < median - K * mad) {
                // low headroom relative to the robust centre -> saturated
                result[i] = LoadState.OVERLOADED;
                overloaded++;
            } else if (headroom > median + K * mad) {
                result[i] = LoadState.UNDERLOADED;
                underloaded++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] classified ", n,
            " of ", hosts.size(), " hosts via MAD (median=", median, ", scaledMad=", mad,
            ") -> overloaded=", overloaded, ", underloaded=", underloaded,
            ", balanced=", (n - overloaded - underloaded));

        return result;
    }

    private double median(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int m = sorted.length;
        if (m % 2 == 1) {
            return sorted[m / 2];
        }
        return (sorted[m / 2 - 1] + sorted[m / 2]) / 2.0;
    }

    @Override
    public String inputSemantic() {
        return "host-mipsHeadroomFraction-instantaneous";
    }

    @Override
    public String outputSemantic() {
        return "host-loadState-medianAbsoluteDeviation";
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

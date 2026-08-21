package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

/**
 * analyser_v4 - VM level, asymmetric fixed-floor / percentile classifier.
 * Strategy: deliberately mixes two different boundary techniques. The
 * underload boundary is a fixed near-zero constant (a VM producing
 * essentially no throughput is idle regardless of what its peers are doing).
 * The overload boundary is instead the 90th percentile of the observed
 * throughput distribution this cycle, so "overloaded" always means "top
 * decile of current fleet throughput", whatever that value happens to be.
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final double UNDERLOAD_FLOOR_MIPS = 1e-6;
    private static final double OVERLOAD_PERCENTILE = 0.90;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        double overloadThreshold = percentile(metrics, OVERLOAD_PERCENTILE);

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            if (metrics[i] <= UNDERLOAD_FLOOR_MIPS) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else if (metrics[i] >= overloadThreshold) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v4] classified ", n,
                " vms by fixed floor + p90 (floor=", UNDERLOAD_FLOOR_MIPS,
                ", p90=", overloadThreshold, ") -> overloaded=", overloadCount,
                ", underloaded=", underloadCount, ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    private double percentile(double[] values, double p) {
        if (values.length == 0) {
            return 0.0;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        if (sorted.length == 1) {
            return sorted[0];
        }
        double rank = p * (sorted.length - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        if (lowerIndex == upperIndex) {
            return sorted[lowerIndex];
        }
        double weight = rank - lowerIndex;
        return sorted[lowerIndex] * (1 - weight) + sorted[upperIndex] * weight;
    }

    @Override
    public String inputSemantic() {
        return "vm-throughput-mips";
    }

    @Override
    public String outputSemantic() {
        return "vm-loadstate-throughput";
    }

    @Override
    public int inputGuid() {
        return 1300;
    }

    @Override
    public int outputGuid() {
        return 2300;
    }
}

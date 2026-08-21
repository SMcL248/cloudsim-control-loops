package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import java.util.Arrays;

/**
 * analyser_v6 - Cloudlet level, Tukey IQR outlier-fence classifier.
 * Strategy: treats a cloudlet as "overloaded" if its remaining length is a
 * statistical high outlier relative to its peers (classic Tukey fence:
 * above Q3 + 1.5*IQR), and "underloaded" if it is a low outlier (below
 * Q1 - 1.5*IQR, i.e. almost finished while peers still have substantial
 * work left). Both fences are derived purely from the observed quartiles.
 */
public class analyser_v6 implements Analyser<double[], LoadState[]> {

    private static final double FENCE_MULTIPLIER = 1.5;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        int n = metrics.length;
        LoadState[] result = new LoadState[n];

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);

        double q1 = percentileFromSorted(sorted, 0.25);
        double q3 = percentileFromSorted(sorted, 0.75);
        double iqr = q3 - q1;

        double upperFence = q3 + FENCE_MULTIPLIER * iqr;
        double lowerFence = q1 - FENCE_MULTIPLIER * iqr;

        int overloadCount = 0;
        int underloadCount = 0;

        for (int i = 0; i < n; i++) {
            if (iqr <= 0.0) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            if (metrics[i] > upperFence) {
                result[i] = LoadState.OVERLOADED;
                overloadCount++;
            } else if (metrics[i] < lowerFence) {
                result[i] = LoadState.UNDERLOADED;
                underloadCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }

        Log.printlnConcat(readSpace.getNow(), ": [analyser_v6] classified ", n,
                " cloudlets by IQR fences (q1=", q1, ", q3=", q3, ", iqr=", iqr,
                ", upperFence=", upperFence, ", lowerFence=", lowerFence,
                ") -> overloaded=", overloadCount, ", underloaded=", underloadCount,
                ", balanced=", (n - overloadCount - underloadCount));

        return result;
    }

    private double percentileFromSorted(double[] sorted, double p) {
        if (sorted.length == 0) {
            return 0.0;
        }
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
        return "cloudlet-remaininglength-mi";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-loadstate-remaininglength";
    }

    @Override
    public int inputGuid() {
        return 1400;
    }

    @Override
    public int outputGuid() {
        return 2400;
    }
}

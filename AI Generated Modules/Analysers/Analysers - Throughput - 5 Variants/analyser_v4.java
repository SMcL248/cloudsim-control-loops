package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;

/**
 * analyser_v4 - VM Normalised Demand, 3-Cluster 1D K-Means
 *
 * Classifies each VM by its normalised demand ratio
 * (getCurrentRequestedTotalMips() / getMips()) using a lightweight 1D
 * k-means clustering pass (Lloyd's algorithm, fixed iteration count) with
 * three centroids instead of a fixed or statistical cutoff. Centroids are
 * seeded at the snapshot's min, mean, and max, then refined by repeated
 * assignment/averaging. The cluster with the lowest final centroid maps to
 * UNDERLOADED, the highest to OVERLOADED, and the middle to BALANCED.
 *
 * This adapts to natural groupings in the data (e.g. a bimodal demand
 * distribution) rather than assuming a symmetric or normally-distributed
 * spread. Requires at least 3 VMs and some spread in the data; otherwise
 * every VM is reported BALANCED.
 *
 * inputGuid  : vm-demand
 * outputGuid : vm-demand-loadstate
 */
public class analyser_v4 implements Analyser<double[], LoadState[]> {

    private static final int MIN_VMS = 3;
    private static final int ITERATIONS = 10;

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] states = new LoadState[n];

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0.0;
        for (double v : metrics) {
            if (v < min) min = v;
            if (v > max) max = v;
            sum += v;
        }
        double mean = (n > 0) ? sum / n : 0.0;

        if (n < MIN_VMS || max == min) {
            for (int i = 0; i < n; i++) {
                states[i] = LoadState.BALANCED;
                Log.printlnConcat(now, ": [analyser_v4] VM ", i,
                        " demand=", metrics[i], " state=BALANCED (insufficient spread)");
            }
            return states;
        }

        double[] centroids = new double[]{min, mean, max};
        int[] assignment = new int[n];

        for (int iter = 0; iter < ITERATIONS; iter++) {
            for (int i = 0; i < n; i++) {
                assignment[i] = nearestCentroid(metrics[i], centroids);
            }

            double[] newSum = new double[3];
            int[] count = new int[3];
            for (int i = 0; i < n; i++) {
                newSum[assignment[i]] += metrics[i];
                count[assignment[i]]++;
            }
            for (int c = 0; c < 3; c++) {
                if (count[c] > 0) centroids[c] = newSum[c] / count[c];
            }
        }

        int lowCluster  = indexOfMin(centroids);
        int highCluster = indexOfMax(centroids);

        for (int i = 0; i < n; i++) {
            int cluster = assignment[i];
            if (cluster == lowCluster) {
                states[i] = LoadState.UNDERLOADED;
            } else if (cluster == highCluster) {
                states[i] = LoadState.OVERLOADED;
            } else {
                states[i] = LoadState.BALANCED;
            }

            Log.printlnConcat(now, ": [analyser_v4] VM ", i,
                    " demand=", metrics[i], " cluster=", cluster,
                    " centroids=[", centroids[0], ",", centroids[1], ",", centroids[2], "]",
                    " state=", states[i]);
        }

        return states;
    }

    private int nearestCentroid(double value, double[] centroids) {
        int best = 0;
        double bestDist = Math.abs(value - centroids[0]);
        for (int c = 1; c < centroids.length; c++) {
            double dist = Math.abs(value - centroids[c]);
            if (dist < bestDist) {
                bestDist = dist;
                best = c;
            }
        }
        return best;
    }

    private int indexOfMin(double[] values) {
        int idx = 0;
        for (int i = 1; i < values.length; i++) if (values[i] < values[idx]) idx = i;
        return idx;
    }

    private int indexOfMax(double[] values) {
        int idx = 0;
        for (int i = 1; i < values.length; i++) if (values[i] > values[idx]) idx = i;
        return idx;
    }

    @Override
    public String inputGuid() {
        return "vm-demand";
    }

    @Override
    public String outputGuid() {
        return "vm-demand-loadstate";
    }
}

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v19 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        if (n < 3) {
            for (int i = 0; i < n; i++) result[i] = LoadState.BALANCED;
        } else {
            int[] labels = kMeans1D(metrics, 3, 25);
            int overCount = 0, underCount = 0;
            for (int i = 0; i < n; i++) {
                if (labels[i] == 0) { result[i] = LoadState.UNDERLOADED; underCount++; }
                else if (labels[i] == 2) { result[i] = LoadState.OVERLOADED; overCount++; }
                else result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v19] classified ", n, " vms via 1D k-means (k=3) clustering (over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    private int[] kMeans1D(double[] data, int k, int maxIter) {
        int m = data.length;
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double[] centroids = new double[k];
        for (int c = 0; c < k; c++) {
            int idx = (int) ((c + 0.5) / k * m);
            if (idx >= m) idx = m - 1;
            centroids[c] = sorted[idx];
        }
        int[] labels = new int[m];
        for (int iter = 0; iter < maxIter; iter++) {
            for (int i = 0; i < m; i++) {
                int best = 0;
                double bestDist = Math.abs(data[i] - centroids[0]);
                for (int c = 1; c < k; c++) {
                    double dist = Math.abs(data[i] - centroids[c]);
                    if (dist < bestDist) { bestDist = dist; best = c; }
                }
                labels[i] = best;
            }
            double[] sums = new double[k];
            int[] counts = new int[k];
            for (int i = 0; i < m; i++) { sums[labels[i]] += data[i]; counts[labels[i]]++; }
            for (int c = 0; c < k; c++) if (counts[c] > 0) centroids[c] = sums[c] / counts[c];
        }
        Integer[] order = new Integer[k];
        for (int c = 0; c < k; c++) order[c] = c;
        final double[] finalCentroids = centroids;
        Arrays.sort(order, (a, b) -> Double.compare(finalCentroids[a], finalCentroids[b]));
        int[] rank = new int[k];
        for (int r = 0; r < k; r++) rank[order[r]] = r;
        int[] remapped = new int[m];
        for (int i = 0; i < m; i++) remapped[i] = rank[labels[i]];
        return remapped;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-kmeans-1d-k3";
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

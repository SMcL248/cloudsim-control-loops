package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v35 implements Analyser<double[], LoadState[]> {

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
            double[] sorted = metrics.clone();
            Arrays.sort(sorted);
            double meanVal = mean(sorted);
            double[] cusum = new double[n + 1];
            cusum[0] = 0.0;
            for (int i = 0; i < n; i++) cusum[i + 1] = cusum[i] + (sorted[i] - meanVal);
            int minIdx = 0;
            for (int i = 1; i <= n; i++) if (cusum[i] < cusum[minIdx]) minIdx = i;
            int maxIdx = minIdx;
            for (int i = minIdx; i <= n; i++) if (cusum[i] > cusum[maxIdx]) maxIdx = i;
            int lowerSortedIdx = Math.max(0, Math.min(minIdx, n - 1));
            int upperSortedIdx = Math.max(0, Math.min(maxIdx == n ? n - 1 : maxIdx, n - 1));
            double lowerBoundary = sorted[lowerSortedIdx];
            double upperBoundary = sorted[upperSortedIdx];
            if (lowerBoundary > upperBoundary) {
                double tmp = lowerBoundary;
                lowerBoundary = upperBoundary;
                upperBoundary = tmp;
            }
            int overCount = 0, underCount = 0;
            for (int i = 0; i < n; i++) {
                if (metrics[i] > upperBoundary) { result[i] = LoadState.OVERLOADED; overCount++; }
                else if (metrics[i] < lowerBoundary) { result[i] = LoadState.UNDERLOADED; underCount++; }
                else result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v35] classified ", n, " hosts via CUSUM cumulative-deviation change-point detection (lower=", lowerBoundary, ", upper=", upperBoundary, ", over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    private double mean(double[] data) {
        double sum = 0.0;
        for (double v : data) sum += v;
        return (data.length > 0) ? sum / data.length : 0.0;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-cusum-changepoint";
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

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v8 implements Analyser<double[], LoadState[]> {

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
            double[] thresholds = otsuTwoThresholds(metrics);
            double t1 = thresholds[0];
            double t2 = thresholds[1];
            int overCount = 0, underCount = 0;
            for (int i = 0; i < n; i++) {
                if (metrics[i] > t2) { result[i] = LoadState.OVERLOADED; overCount++; }
                else if (metrics[i] <= t1) { result[i] = LoadState.UNDERLOADED; underCount++; }
                else result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v8] classified ", n, " hosts via Otsu two-threshold variance split (t1=", t1, ", t2=", t2, ", over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    private double[] otsuTwoThresholds(double[] data) {
        int m = data.length;
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        double[] prefixSum = new double[m + 1];
        for (int i = 0; i < m; i++) prefixSum[i + 1] = prefixSum[i] + sorted[i];
        double totalSum = prefixSum[m];
        double totalMean = totalSum / m;
        double bestVar = -1.0;
        int bestI = 0, bestJ = m;
        for (int i = 1; i < m; i++) {
            double sumA = prefixSum[i];
            double meanA = sumA / i;
            for (int j = i + 1; j <= m; j++) {
                double sumB = prefixSum[j] - prefixSum[i];
                int countB = j - i;
                double meanB = sumB / countB;
                int countC = m - j;
                double sumC = totalSum - prefixSum[j];
                double meanC = (countC > 0) ? sumC / countC : totalMean;
                double var = i * (meanA - totalMean) * (meanA - totalMean)
                        + countB * (meanB - totalMean) * (meanB - totalMean)
                        + countC * (meanC - totalMean) * (meanC - totalMean);
                if (var > bestVar) {
                    bestVar = var;
                    bestI = i;
                    bestJ = j;
                }
            }
        }
        double t1 = sorted[Math.max(0, bestI - 1)];
        int t2Idx = Math.min(m - 1, Math.max(bestJ - 1, bestI));
        double t2 = sorted[t2Idx];
        return new double[]{t1, t2};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-otsu-two-threshold";
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

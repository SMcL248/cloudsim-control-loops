package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v3 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);
        double q1 = percentile(sorted, 25.0);
        double q3 = percentile(sorted, 75.0);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] > q3) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < q1) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v3] classified ", n, " cloudlets via quartile split (q1=", q1, ", q3=", q3, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double percentile(double[] sortedData, double p) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m == 1) return sortedData[0];
        double rank = (p / 100.0) * (m - 1);
        int lowIdx = (int) Math.floor(rank);
        int highIdx = (int) Math.ceil(rank);
        if (lowIdx == highIdx) return sortedData[lowIdx];
        double frac = rank - lowIdx;
        return sortedData[lowIdx] * (1.0 - frac) + sortedData[highIdx] * frac;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-remaining-length-fraction";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-quartile-split";
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

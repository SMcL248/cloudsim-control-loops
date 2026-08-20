package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v37 implements Analyser<double[], LoadState[]> {

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
        double upperBound = percentile(sorted, 90.0);
        double med = median(sorted);
        double madScaled = medianAbsoluteDeviation(metrics, med) * 1.4826;
        double lowerBound = med - 1.5 * madScaled;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] > upperBound) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < lowerBound) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v37] classified ", n, " vms via hybrid percentile-upper / MAD-lower asymmetric bands (over=", overCount, ", under=", underCount, ").");
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
    private double median(double[] sortedData) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m % 2 == 1) return sortedData[m / 2];
        return (sortedData[m / 2 - 1] + sortedData[m / 2]) / 2.0;
    }
    private double medianAbsoluteDeviation(double[] data, double medianVal) {
        double[] absDev = new double[data.length];
        for (int i = 0; i < data.length; i++) absDev[i] = Math.abs(data[i] - medianVal);
        Arrays.sort(absDev);
        return median(absDev);
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-hybrid-percentile-mad-band";
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

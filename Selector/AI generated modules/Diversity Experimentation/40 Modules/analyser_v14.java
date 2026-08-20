package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v14 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        if (n < 2) {
            for (int i = 0; i < n; i++) result[i] = LoadState.BALANCED;
        } else {
            int bins = Math.max(3, Math.min(10, n));
            double[] bounds = histogramValleyThresholds(metrics, bins);
            double lowerBound = bounds[0];
            double upperBound = bounds[1];
            int overCount = 0, underCount = 0;
            for (int i = 0; i < n; i++) {
                if (metrics[i] > upperBound) { result[i] = LoadState.OVERLOADED; overCount++; }
                else if (metrics[i] < lowerBound) { result[i] = LoadState.UNDERLOADED; underCount++; }
                else result[i] = LoadState.BALANCED;
            }
            Log.printlnConcat(now, ": [analyser_v14] classified ", n, " hosts via histogram valley density split (lower=", lowerBound, ", upper=", upperBound, ", over=", overCount, ", under=", underCount, ").");
        }
        return result;
    }

    private double[] histogramValleyThresholds(double[] data, int bins) {
        double minV = data[0], maxV = data[0];
        for (double v : data) { if (v < minV) minV = v; if (v > maxV) maxV = v; }
        double range = maxV - minV;
        if (range < 1e-9) return new double[]{minV, maxV};
        int[] counts = new int[bins];
        for (double v : data) {
            int idx = (int) ((v - minV) / range * bins);
            if (idx >= bins) idx = bins - 1;
            if (idx < 0) idx = 0;
            counts[idx]++;
        }
        int peakBin = 0;
        for (int i = 1; i < bins; i++) if (counts[i] > counts[peakBin]) peakBin = i;
        int lowValleyBin = 0;
        for (int i = 1; i < peakBin; i++) if (counts[i] <= counts[lowValleyBin]) lowValleyBin = i;
        int highValleyBin = peakBin;
        for (int i = peakBin + 1; i < bins; i++) if (counts[i] <= counts[highValleyBin]) highValleyBin = i;
        double lowerBound = minV + ((double) lowValleyBin / bins) * range;
        double upperBound = minV + ((double) (highValleyBin + 1) / bins) * range;
        return new double[]{lowerBound, upperBound};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-histogram-valley-split";
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

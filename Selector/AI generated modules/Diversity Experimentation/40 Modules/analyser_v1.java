package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v1 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double meanVal = mean(metrics);
        double stdVal = stddev(metrics, meanVal);
        double upper = meanVal + stdVal;
        double lower = meanVal - stdVal;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (metrics[i] > upper) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < lower) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v1] classified ", n, " hosts via mean/std z-score (mean=", meanVal, ", std=", stdVal, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double mean(double[] data) {
        double sum = 0.0;
        for (double v : data) sum += v;
        return (data.length > 0) ? sum / data.length : 0.0;
    }
    private double stddev(double[] data, double meanVal) {
        double sq = 0.0;
        for (double v : data) sq += (v - meanVal) * (v - meanVal);
        return (data.length > 0) ? Math.sqrt(sq / data.length) : 0.0;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-zscore-mean-std";
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

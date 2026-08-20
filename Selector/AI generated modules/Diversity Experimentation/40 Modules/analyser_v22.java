package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v22 implements Analyser<double[], LoadState[]> {

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
        double skew = skewness(metrics, meanVal, stdVal);
        double upperK = 1.0 - Math.max(-0.5, Math.min(0.5, skew * 0.2));
        double lowerK = 1.0 + Math.max(-0.5, Math.min(0.5, skew * 0.2));
        double upper = meanVal + upperK * stdVal;
        double lower = meanVal - lowerK * stdVal;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (metrics[i] > upper) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < lower) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v22] classified ", n, " hosts via skewness-adjusted asymmetric banding (skew=", skew, ", over=", overCount, ", under=", underCount, ").");
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
    private double skewness(double[] data, double meanVal, double stdVal) {
        if (stdVal < 1e-9) return 0.0;
        double sum = 0.0;
        for (double v : data) {
            double z = (v - meanVal) / stdVal;
            sum += z * z * z;
        }
        return sum / data.length;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-skew-adjusted-banding";
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

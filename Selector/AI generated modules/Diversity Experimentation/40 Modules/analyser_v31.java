package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v31 implements Analyser<double[], LoadState[]> {

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
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            double z = (metrics[i] - meanVal) / stdVal;
            double cdf = normalCdf(z);
            if (cdf > 0.90) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (cdf < 0.10) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v31] classified ", n, " vms via normal-CDF probability threshold (over=", overCount, ", under=", underCount, ").");
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
    private double normalCdf(double z) {
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937
                + t * (-1.821255978 + t * 1.330274429))));
        double pdf = Math.exp(-0.5 * z * z) / Math.sqrt(2.0 * Math.PI);
        double cdf = 1.0 - pdf * poly;
        return (z >= 0) ? cdf : 1.0 - cdf;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-normal-cdf-probability";
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

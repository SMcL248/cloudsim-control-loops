package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v38 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double maxVal = metrics[0];
        for (double v : metrics) if (v > maxVal) maxVal = v;
        double meanVal = mean(metrics);
        double temperature = stddev(metrics, meanVal);
        if (temperature < 1e-6) temperature = 1.0;
        double[] expVals = new double[n];
        double sumExp = 0.0;
        for (int i = 0; i < n; i++) {
            expVals[i] = Math.exp((metrics[i] - maxVal) / temperature);
            sumExp += expVals[i];
        }
        double uniform = 1.0 / n;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double weight = expVals[i] / sumExp;
            double relative = weight / uniform;
            if (relative > 1.5) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (relative < 0.5) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v38] classified ", n, " hosts via softmax-weighted relative concentration (temperature=", temperature, ", over=", overCount, ", under=", underCount, ").");
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
        return "host-load-state-softmax-relative-weight";
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

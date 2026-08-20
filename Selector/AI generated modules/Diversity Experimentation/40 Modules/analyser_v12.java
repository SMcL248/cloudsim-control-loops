package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v12 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] score = new double[n];
        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            int peCount = readSpace.getHostPeCount(host);
            boolean hasFree = readSpace.hostHasFreePe(host);
            double peFactor = (peCount > 0) ? 1.0 / peCount : 1.0;
            double saturationPenalty = hasFree ? 0.0 : 0.15;
            score[i] = metrics[i] * (1.0 + peFactor) + saturationPenalty;
        }
        double meanVal = mean(score);
        double stdVal = stddev(score, meanVal);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (score[i] > meanVal + stdVal) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (score[i] < meanVal - stdVal) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v12] classified ", n, " hosts via PE-saturation weighted composite score (over=", overCount, ", under=", underCount, ").");
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
        return "host-load-state-pe-saturation-composite";
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

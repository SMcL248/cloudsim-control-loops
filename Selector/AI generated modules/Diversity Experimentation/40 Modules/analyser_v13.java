package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v13 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        List<Double> activeVals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostFailed(host) || readSpace.isHostPermanentlyDead(host) || readSpace.isHostPoweredDown(host)) continue;
            activeVals.add(metrics[i]);
        }
        double[] activeArr = new double[activeVals.size()];
        for (int i = 0; i < activeArr.length; i++) activeArr[i] = activeVals.get(i);
        double meanVal = activeArr.length > 0 ? mean(activeArr) : 0.0;
        double stdVal = activeArr.length > 0 ? stddev(activeArr, meanVal) : 0.0;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            if (readSpace.isHostPermanentlyDead(host) || readSpace.isHostFailed(host)) {
                result[i] = LoadState.UNDERLOADED;
                underCount++;
                continue;
            }
            if (readSpace.isHostPoweredDown(host)) {
                result[i] = LoadState.BALANCED;
                continue;
            }
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (metrics[i] > meanVal + stdVal) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < meanVal - stdVal) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v13] classified ", n, " hosts via failure-aware filtered baseline (activeBaselineMean=", meanVal, ", over=", overCount, ", under=", underCount, ").");
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
        return "host-load-state-failure-aware-baseline";
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

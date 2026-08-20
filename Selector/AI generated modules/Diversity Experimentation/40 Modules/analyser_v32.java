package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v32 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            xs[i] = metrics[i];
            ys[i] = readSpace.getHostPower(host);
        }
        double meanX = mean(xs);
        double meanY = mean(ys);
        double num = 0.0, den = 0.0;
        for (int i = 0; i < n; i++) {
            num += (xs[i] - meanX) * (ys[i] - meanY);
            den += (xs[i] - meanX) * (xs[i] - meanX);
        }
        double b = (den > 1e-9) ? num / den : 0.0;
        double a = meanY - b * meanX;
        double[] residuals = new double[n];
        for (int i = 0; i < n; i++) residuals[i] = ys[i] - (a + b * xs[i]);
        double residMean = mean(residuals);
        double residStd = stddev(residuals, residMean);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (residStd < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            double z = (residuals[i] - residMean) / residStd;
            if (z > 1.0) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (z < -1.0) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v32] classified ", n, " hosts via utilization-vs-power regression residual anomaly (slope=", b, ", over=", overCount, ", under=", underCount, ").");
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
        return "host-load-state-power-regression-residual";
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

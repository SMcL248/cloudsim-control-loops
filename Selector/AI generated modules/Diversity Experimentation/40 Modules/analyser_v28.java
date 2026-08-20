package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v28 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double sumRecip = 0.0;
        int validCount = 0;
        for (double v : metrics) { if (v > 1e-9) { sumRecip += 1.0 / v; validCount++; } }
        double harmonicMean = (validCount > 0 && sumRecip > 1e-9) ? validCount / sumRecip : 0.0;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double ratio = (harmonicMean > 1e-9) ? metrics[i] / harmonicMean : 1.0;
            if (ratio < 0.5) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (ratio > 1.8) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v28] classified ", n, " cloudlets via harmonic-mean completion-rate baseline (harmonicMean=", harmonicMean, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-completion-rate-mi-per-second";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-harmonic-mean-baseline";
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

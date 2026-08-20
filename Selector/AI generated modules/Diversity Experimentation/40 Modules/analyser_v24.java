package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v24 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] stats = iterativeSigmaClip(metrics, 2.0, 5);
        double robMean = stats[0];
        double robStd = stats[1];
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (robStd < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (metrics[i] > robMean + robStd) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < robMean - robStd) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v24] classified ", n, " vms via iterative sigma-clipped robust mean/std (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double[] iterativeSigmaClip(double[] data, double kappa, int maxIter) {
        List<Double> active = new ArrayList<>();
        for (double v : data) active.add(v);
        double meanVal = 0.0, stdVal = 0.0;
        for (int iter = 0; iter < maxIter; iter++) {
            double sum = 0.0;
            for (double v : active) sum += v;
            meanVal = (active.size() > 0) ? sum / active.size() : 0.0;
            double sq = 0.0;
            for (double v : active) sq += (v - meanVal) * (v - meanVal);
            stdVal = (active.size() > 0) ? Math.sqrt(sq / active.size()) : 0.0;
            List<Double> next = new ArrayList<>();
            for (double v : active) {
                if (stdVal < 1e-9 || Math.abs(v - meanVal) <= kappa * stdVal) next.add(v);
            }
            if (next.size() == active.size() || next.isEmpty()) break;
            active = next;
        }
        return new double[]{meanVal, stdVal};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-iterative-sigma-clip";
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

package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v11 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] stats = trimmedStats(metrics, 0.10);
        double tMean = stats[0];
        double tStd = stats[1];
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (tStd < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            double z = (metrics[i] - tMean) / tStd;
            if (z > 1.0) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (z < -1.0) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v11] classified ", n, " hosts via 10%-trimmed mean/std (trimmedMean=", tMean, ", trimmedStd=", tStd, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double[] trimmedStats(double[] data, double trimFraction) {
        double[] sorted = data.clone();
        Arrays.sort(sorted);
        int m = sorted.length;
        int trimCount = (int) Math.floor(m * trimFraction);
        int start = trimCount;
        int end = m - trimCount;
        if (end <= start) { start = 0; end = m; }
        double sum = 0.0;
        int count = 0;
        for (int i = start; i < end; i++) { sum += sorted[i]; count++; }
        double tMean = (count > 0) ? sum / count : 0.0;
        double sq = 0.0;
        for (int i = start; i < end; i++) sq += (sorted[i] - tMean) * (sorted[i] - tMean);
        double tStd = (count > 0) ? Math.sqrt(sq / count) : 0.0;
        return new double[]{tMean, tStd};
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-trimmed-mean-std";
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

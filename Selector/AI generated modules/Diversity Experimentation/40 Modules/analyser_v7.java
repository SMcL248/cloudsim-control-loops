package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v7 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] sorted = metrics.clone();
        Arrays.sort(sorted);
        double q1 = percentile(sorted, 25.0);
        double q3 = percentile(sorted, 75.0);
        double iqr = q3 - q1;
        double upperFence = q3 + 1.5 * iqr;
        double lowerFence = q1 - 1.5 * iqr;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] > upperFence) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < lowerFence) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v7] classified ", n, " vms via Tukey 1.5*IQR outlier fences (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double percentile(double[] sortedData, double p) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m == 1) return sortedData[0];
        double rank = (p / 100.0) * (m - 1);
        int lowIdx = (int) Math.floor(rank);
        int highIdx = (int) Math.ceil(rank);
        if (lowIdx == highIdx) return sortedData[lowIdx];
        double frac = rank - lowIdx;
        return sortedData[lowIdx] * (1.0 - frac) + sortedData[highIdx] * frac;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-tukey-iqr-fence";
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

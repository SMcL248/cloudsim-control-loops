package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v2 implements Analyser<double[], LoadState[]> {

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
        double med = median(sorted);
        double madRaw = medianAbsoluteDeviation(metrics, med);
        double madScaled = madRaw * 1.4826;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (madScaled < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            double z = (metrics[i] - med) / madScaled;
            if (z > 1.0) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (z < -1.0) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v2] classified ", n, " vms via median/MAD robust score (median=", med, ", mad=", madScaled, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double median(double[] sortedData) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m % 2 == 1) return sortedData[m / 2];
        return (sortedData[m / 2 - 1] + sortedData[m / 2]) / 2.0;
    }
    private double medianAbsoluteDeviation(double[] data, double medianVal) {
        double[] absDev = new double[data.length];
        for (int i = 0; i < data.length; i++) absDev[i] = Math.abs(data[i] - medianVal);
        Arrays.sort(absDev);
        return median(absDev);
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-median-mad";
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

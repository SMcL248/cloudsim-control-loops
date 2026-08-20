package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.*;

public class analyser_v26 implements Analyser<double[], LoadState[]> {

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
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double ratio = (med > 1e-9) ? metrics[i] / med : 1.0;
            if (ratio > 2.0) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (ratio < 0.5) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v26] classified ", n, " cloudlets via job-size ratio to population median (median=", med, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    private double median(double[] sortedData) {
        int m = sortedData.length;
        if (m == 0) return 0.0;
        if (m % 2 == 1) return sortedData[m / 2];
        return (sortedData[m / 2 - 1] + sortedData[m / 2]) / 2.0;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-total-length-mi";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-size-relative-median";
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

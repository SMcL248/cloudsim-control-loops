package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v23 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<GuestEntity> vms = readSpace.getVmList();
        double[] perPe = new double[n];
        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);
            int peCount = readSpace.getVmNumberOfPes(vm);
            perPe[i] = (peCount > 0) ? metrics[i] / peCount : metrics[i];
        }
        double meanVal = mean(perPe);
        double stdVal = stddev(perPe, meanVal);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (stdVal < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            if (perPe[i] > meanVal + stdVal) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (perPe[i] < meanVal - stdVal) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v23] classified ", n, " vms via per-PE normalized z-score (over=", overCount, ", under=", underCount, ").");
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
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-per-pe-normalized-zscore";
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

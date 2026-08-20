package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v17 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<GuestEntity> vms = readSpace.getVmList();
        List<Double> allocatedVals = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!readSpace.isVmBeingInstantiated(vms.get(i))) allocatedVals.add(metrics[i]);
        }
        double[] allocArr = new double[allocatedVals.size()];
        for (int i = 0; i < allocArr.length; i++) allocArr[i] = allocatedVals.get(i);
        Arrays.sort(allocArr);
        double q1 = allocArr.length > 0 ? percentile(allocArr, 25.0) : 0.0;
        double q3 = allocArr.length > 0 ? percentile(allocArr, 75.0) : 0.0;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmBeingInstantiated(vm)) { result[i] = LoadState.UNDERLOADED; underCount++; continue; }
            if (metrics[i] > q3) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < q1) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v17] classified ", n, " vms via instantiation-aware quartile split on allocated subset (over=", overCount, ", under=", underCount, ").");
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
        return "vm-load-state-instantiation-aware-quartile";
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

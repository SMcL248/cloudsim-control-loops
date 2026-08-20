package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import java.util.stream.*;
import java.util.*;

public class analyser_v27 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        double[] uniqueVals = Arrays.stream(metrics).distinct().sorted().toArray();
        int tierCount = uniqueVals.length;
        Map<Double, Integer> tierIndex = new HashMap<>();
        for (int t = 0; t < tierCount; t++) tierIndex.put(uniqueVals[t], t);
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            int tier = tierIndex.get(metrics[i]);
            double position = (tierCount > 1) ? (double) tier / (tierCount - 1) : 0.5;
            if (position >= 0.75) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (position <= 0.25) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v27] classified ", n, " cloudlets via ordinal PE-demand tier position (tierCount=", tierCount, ", over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-requested-pe-count";
    }

    @Override
    public String outputSemantic() {
        return "cloudlet-load-state-ordinal-pe-tier";
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

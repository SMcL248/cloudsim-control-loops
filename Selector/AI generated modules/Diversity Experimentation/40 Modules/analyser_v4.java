package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v4 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        final double UNDER_THRESHOLD = 0.30;
        final double OVER_THRESHOLD = 0.80;
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            if (metrics[i] > OVER_THRESHOLD) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (metrics[i] < UNDER_THRESHOLD) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v4] classified ", n, " hosts via fixed engineering thresholds (under<", UNDER_THRESHOLD, ", over>", OVER_THRESHOLD, ", overCount=", overCount, ", underCount=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-fixed-engineering-thresholds";
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

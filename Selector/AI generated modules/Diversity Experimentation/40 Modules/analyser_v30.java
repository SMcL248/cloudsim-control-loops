package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v30 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            double rho = Math.max(0.0, Math.min(0.995, metrics[i]));
            double expectedQueueLength = rho / (1.0 - rho);
            if (expectedQueueLength > 4.0) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (expectedQueueLength < 0.15) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v30] classified ", n, " hosts via M/M/1 queueing-theory blow-up factor (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-queueing-blowup-factor";
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

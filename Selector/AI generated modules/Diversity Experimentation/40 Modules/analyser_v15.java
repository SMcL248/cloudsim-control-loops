package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v15 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<GuestEntity> vms = readSpace.getVmList();
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            GuestEntity vm = vms.get(i);
            double histMean = readSpace.getVmUtilizationMean(vm);
            double histMadRaw = readSpace.getVmUtilizationMad(vm);
            double vmMips = readSpace.getVmMips(vm);
            double histMadScaled = histMadRaw * vmMips;
            if (histMadScaled < 1e-9) { result[i] = LoadState.BALANCED; continue; }
            double z = (metrics[i] - histMean) / histMadScaled;
            if (z > 1.5) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (z < -1.5) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v15] classified ", n, " vms via self-referential historical mean/MAD comparison (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-instant";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-historical-self-mad-z";
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

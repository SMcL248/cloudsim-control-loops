package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v5 implements Analyser<double[], LoadState[]> {

    @Override
    public LoadState[] analyse(double[] metrics, ReadSpace readSpace) {
        double now = readSpace.getNow();
        int n = metrics.length;
        LoadState[] result = new LoadState[n];
        if (n == 0) {
            return result;
        }

        List<HostEntity> hosts = readSpace.getAllHosts();
        int overCount = 0, underCount = 0;
        for (int i = 0; i < n; i++) {
            HostEntity host = hosts.get(i);
            double maxPower = readSpace.getHostMaxPower(host);
            double ratio = (maxPower > 1e-9) ? metrics[i] / maxPower : 0.0;
            if (ratio > 0.75) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (ratio < 0.25) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v5] classified ", n, " hosts via power-draw ratio to max power (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-power-draw-watts";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-power-ratio-to-max";
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

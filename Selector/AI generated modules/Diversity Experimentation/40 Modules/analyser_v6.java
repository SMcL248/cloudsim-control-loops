package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.HostEntity;
import java.util.*;

public class analyser_v6 implements Analyser<double[], LoadState[]> {

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
            double total = readSpace.getHostTotalMips(host);
            double headroomRatio = (total > 1e-9) ? metrics[i] / total : 1.0;
            if (headroomRatio < 0.15) { result[i] = LoadState.OVERLOADED; overCount++; }
            else if (headroomRatio > 0.60) { result[i] = LoadState.UNDERLOADED; underCount++; }
            else result[i] = LoadState.BALANCED;
        }
        Log.printlnConcat(now, ": [analyser_v6] classified ", n, " hosts via inverted MIPS-headroom ratio (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "host-mips-headroom-absolute";
    }

    @Override
    public String outputSemantic() {
        return "host-load-state-inverse-headroom-ratio";
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

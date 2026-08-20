package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.*;

public class analyser_v20 implements Analyser<double[], LoadState[]> {

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
            double currentMips = readSpace.getVmMips(vm);
            double nextTier = readSpace.getNextMipsTier(vm);
            double utilRatio = (currentMips > 1e-9) ? metrics[i] / currentMips : 0.0;
            if (utilRatio > 0.85) {
                result[i] = LoadState.OVERLOADED;
                overCount++;
            } else if (utilRatio < 0.25 && nextTier > 0) {
                result[i] = LoadState.UNDERLOADED;
                underCount++;
            } else if (utilRatio < 0.25) {
                result[i] = LoadState.UNDERLOADED;
                underCount++;
            } else {
                result[i] = LoadState.BALANCED;
            }
        }
        Log.printlnConcat(now, ": [analyser_v20] classified ", n, " vms via tier-ladder-aware MIPS ratio (over=", overCount, ", under=", underCount, ").");
        return result;
    }

    @Override
    public String inputSemantic() {
        return "vm-current-mips-utilization";
    }

    @Override
    public String outputSemantic() {
        return "vm-load-state-tier-ladder-ratio";
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

package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.core.HostEntity;
import org.cloudbus.cloudsim.core.PowerGuestEntity;
import org.cloudbus.cloudsim.core.PowerHostEntity;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class planner_v11 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int[] mipsTiers = readSpace.getMipsTiers();
        int vmId = -1;
        int tierIndex = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            double nextTier = readSpace.getNextMipsTier(vm);
            if (nextTier < 0) continue;
            int nextIndex = -1;
            for (int t = 0; t < mipsTiers.length; t++) {
                if ((int) Math.round(nextTier) == mipsTiers[t]) { nextIndex = t; break; }
            }
            if (nextIndex == -1) continue;
            double mad = readSpace.getVmUtilizationMad(vm) * readSpace.getVmMips(vm);
            double mean = readSpace.getVmUtilizationMean(vm) * readSpace.getVmMips(vm);
            boolean bursty = mean > 0 && mad > mean * 0.3;
            tierIndex = bursty ? Math.min(mipsTiers.length - 1, nextIndex + 1) : nextIndex;
            vmId = readSpace.getId(vm);
            break;
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
            tierIndex = mipsTiers.length > 0 ? mipsTiers.length - 1 : 0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v11] Variance-Aware Vertical Scaling for VM ", vmId, " to tier ", tierIndex, ".");
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-variance-aware-mips";
    }

    @Override
    public String outputSemantic() {
        return "requestMipsScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3005;
    }

}

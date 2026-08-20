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

public class planner_v14 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int[] mipsTiers = readSpace.getMipsTiers();
        int vmId = -1;
        int tierIndex = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            double mad = readSpace.getVmUtilizationMad(vm);
            double mean = readSpace.getVmUtilizationMean(vm);
            boolean stable = mean >= 0 && mad < 0.1;
            if (!stable) continue;
            double currentMips = readSpace.getVmMips(vm);
            int currentIndex = -1;
            for (int t = 0; t < mipsTiers.length; t++) {
                if ((int) Math.round(currentMips) == mipsTiers[t]) { currentIndex = t; break; }
            }
            if (currentIndex <= 0) continue;
            vmId = readSpace.getId(vm);
            tierIndex = currentIndex - 1;
            break;
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
            tierIndex = 0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v14] Stability-Gated Downscaling for VM ", vmId, " to tier ", tierIndex, ".");
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-stability-gated-downscale";
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

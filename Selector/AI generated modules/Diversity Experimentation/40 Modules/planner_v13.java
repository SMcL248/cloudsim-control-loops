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

public class planner_v13 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int[] mipsTiers = readSpace.getMipsTiers();
        int vmId = -1;
        double bestUtil = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            double util = readSpace.getVmCpuUtil(vm);
            if (util > bestUtil) {
                bestUtil = util;
                vmId = readSpace.getId(vm);
            }
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        int tierIndex = mipsTiers.length > 0 ? mipsTiers.length - 1 : 0;
        Log.printlnConcat(readSpace.getNow(), ": [planner_v13] Aggressive Max-Tier Scaling for VM ", vmId, " (util=", bestUtil, ") to max tier ", tierIndex, ".");
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-aggressive-max-mips";
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

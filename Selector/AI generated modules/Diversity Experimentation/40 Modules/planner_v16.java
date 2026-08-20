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

public class planner_v16 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int n = Math.min(diagnosis.length, vms.size());
        int[] bwTiers = readSpace.getBwTiers();
        int vmId = -1;
        int bestCount = -1;
        for (int i = 0; i < n; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            int count = readSpace.getVmCloudletList(vm).size();
            if (count > bestCount) {
                bestCount = count;
                vmId = readSpace.getId(vm);
            }
        }
        int tierIndex = -1;
        if (vmId != -1) {
            GuestEntity vm = readSpace.getVmById(vmId);
            double nextBw = readSpace.getNextBwTier(vm);
            if (nextBw >= 0) {
                for (int t = 0; t < bwTiers.length; t++) {
                    if ((int) Math.round(nextBw) == bwTiers[t]) { tierIndex = t; break; }
                }
            }
        }
        if (vmId == -1 && !vms.isEmpty()) {
            vmId = readSpace.getId(vms.get(0));
        }
        if (tierIndex == -1) {
            tierIndex = 0;
        }
        Log.printlnConcat(readSpace.getNow(), ": [planner_v16] Workload-Density BW Scaling for VM ", vmId, " (cloudlets=", bestCount, ") to tier ", tierIndex, ".");
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-workload-density-bw";
    }

    @Override
    public String outputSemantic() {
        return "requestBwScaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3007;
    }

}

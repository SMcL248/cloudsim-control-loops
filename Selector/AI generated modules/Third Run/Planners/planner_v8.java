package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

import java.util.List;

// Strategy: pure power, safety-checked. Diagnosis is per-VM. Reclaims a core from the most underloaded
// VM, but only if it would still retain at least one PE afterwards -- stripping a VM bare would strand
// any future workload assigned to it.
public class planner_v8 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || vms == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] diagnosis/VM list mismatch, no-op");
            return new int[0];
        }

        int targetIndex = -1;
        double bestUtil = Double.MAX_VALUE;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) continue;
            double util = readSpace.getVmCpuUtil(vm);
            if (util < bestUtil) {
                bestUtil = util;
                targetIndex = i;
            }
        }

        if (targetIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] no reclaimable underloaded VM found, no-op");
            return new int[0];
        }

        int vmId = readSpace.getId(vms.get(targetIndex));

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] deallocating a PE from underloaded VM ", vmId, " to cut power draw");

        return new int[] { vmId };
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-load-state";
    }

    @Override
    public String outputSemantic() {
        return "requestPeDeallocation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3009;
    }
}

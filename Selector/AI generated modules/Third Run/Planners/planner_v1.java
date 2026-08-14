package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import org.cloudbus.cloudsim.Cloudlet;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Strategy: pure throughput. Diagnosis is per-cloudlet, describing whether the VM currently hosting
// each active cloudlet is overloaded/underloaded/balanced. Relieves the overloaded-context cloudlet
// with the largest remaining length (biggest impact on makespan) by moving it to the least-utilised
// VM available. No power consideration.
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {

        List<Cloudlet> cloudlets = readSpace.getActiveCloudlets();

        if (diagnosis == null || cloudlets == null || diagnosis.length != cloudlets.size()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] diagnosis/cloudlet list mismatch, no-op");
            return new int[0];
        }

        // Map each cloudlet id to its current hosting VM id.
        Map<Integer, Integer> cloudletToVm = new HashMap<Integer, Integer>();
        List<GuestEntity> vms = readSpace.getVmList();
        for (GuestEntity vm : vms) {
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            for (Cloudlet cl : owned) {
                cloudletToVm.put(readSpace.getId(cl), readSpace.getId(vm));
            }
        }

        // Find the overloaded-context cloudlet with the largest remaining length: relieving it
        // matters most for throughput.
        int worstIndex = -1;
        long worstRemaining = -1;
        for (int i = 0; i < diagnosis.length; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                long remaining = readSpace.getRemainingLength(cloudlets.get(i));
                if (remaining > worstRemaining) {
                    worstRemaining = remaining;
                    worstIndex = i;
                }
            }
        }

        if (worstIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] no overloaded-context cloudlet found, no-op");
            return new int[0];
        }

        Cloudlet target = cloudlets.get(worstIndex);
        int cloudletId = readSpace.getId(target);
        Integer fromVmId = cloudletToVm.get(cloudletId);

        if (fromVmId == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] could not resolve source VM for cloudlet ", cloudletId, ", no-op");
            return new int[0];
        }

        // Pick the least utilised, non-migrating, non-instantiating VM as destination.
        int toVmId = -1;
        double bestUtil = Double.MAX_VALUE;
        for (GuestEntity vm : vms) {
            int vmId = readSpace.getId(vm);
            if (vmId == fromVmId) continue;
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;
            double util = readSpace.getVmCpuUtil(vm);
            if (util < bestUtil) {
                bestUtil = util;
                toVmId = vmId;
            }
        }

        if (toVmId == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] no destination VM available, no-op");
            return new int[0];
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] moving cloudlet ", cloudletId, " from VM ", fromVmId, " to VM ", toVmId);

        return new int[] { cloudletId, fromVmId, toVmId };
    }

    @Override
    public String inputSemantic() {
        return "cloudlet-context-load-state";
    }

    @Override
    public String outputSemantic() {
        return "moveCloudlet";
    }

    @Override
    public int inputGuid() {
        return 2400;
    }

    @Override
    public int outputGuid() {
        return 3001;
    }
}

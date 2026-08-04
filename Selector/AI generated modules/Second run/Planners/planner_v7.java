package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level planner. diagnosis[i] is the load state of readSpace.getVmList().get(i).
// Goal: minimise power (total energy / makespan).
// Strategy: a VM classified UNDERLOADED with a consistently low mean
// utilisation is carrying more allocated PEs than its workload needs. PEs
// held on a host directly factor into that host's power draw, so
// deallocate one PE from the least-utilised such VM. Never deallocate down
// to zero PEs.
public class planner_v7 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v7";
    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3009;
    private static final double LOW_UTILIZATION_THRESHOLD = 0.2;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/VM size mismatch, no-op");
            return new int[]{-1};
        }

        GuestEntity bestCandidate = null;
        double lowestUtil = Double.MAX_VALUE;

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;
            if (readSpace.getVmNumberOfPes(vm) <= 1) continue; // keep at least one PE

            double util = readSpace.getVmUtilizationMean(vm);
            if (util > LOW_UTILIZATION_THRESHOLD) continue;

            if (util < lowestUtil) {
                lowestUtil = util;
                bestCandidate = vm;
            }
        }

        if (bestCandidate == null) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no low-utilisation VM eligible for PE deallocation, no-op");
            return new int[]{-1};
        }

        int vmId = readSpace.getId(bestCandidate);
        Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan deallocate PE from VM ", vmId,
                " (mean util ", lowestUtil, ")");
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-cpu-utilization-idle";
    }

    @Override
    public String outputSemantic() {
        return "requestpedeallocation";
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    @Override
    public int outputGuid() {
        return OUTPUT_GUID;
    }
}

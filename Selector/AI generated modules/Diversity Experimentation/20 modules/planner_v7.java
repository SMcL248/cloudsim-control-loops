package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) {
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v7] VM ", vmId, " is underloaded with multiple PEs, deallocating one");
            return new int[] { vmId };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v7] no over-provisioned underloaded VM found for PE deallocation");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-cpu-underload-multi-pe";
    }

    @Override
    public String outputSemantic() {
        return "pe-deallocate";
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

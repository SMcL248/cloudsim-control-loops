package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v14 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            if (!readSpace.getVmCloudletList(vm).isEmpty()) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v14] VM ", vmId, " is idle and underloaded, requesting destruction");
            return new int[] { vmId };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v14] no idle underloaded VM found for teardown");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-idle-teardown-candidate";
    }

    @Override
    public String outputSemantic() {
        return "vm-destruction";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3004;
    }
}

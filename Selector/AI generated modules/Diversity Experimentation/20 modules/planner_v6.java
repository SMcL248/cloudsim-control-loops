package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.getNextMipsTier(vm) >= 0) {
                // Still has vertical MIPS headroom; leave that path to a MIPS-scaling planner.
                continue;
            }
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v6] VM ", vmId, " is MIPS-tier-saturated, requesting extra PE");
            return new int[] { vmId };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v6] no MIPS-saturated overloaded VM found for PE allocation");
        return new int[0];
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-cpu-overload-mips-saturated";
    }

    @Override
    public String outputSemantic() {
        return "pe-allocate";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3008;
    }
}

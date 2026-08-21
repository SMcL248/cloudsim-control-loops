package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 4: Idle-VM reaping for safe scale-in.
 * Strategy: read VM-level diagnosis. An UNDERLOADED VM is only a
 * destruction candidate if it is currently carrying zero workload - this
 * guards against the destructive side-effect of requestVmDestruction
 * (loss of any allocated Cloudlets) by only ever reaping guests that have
 * nothing to lose.
 */
public class planner_v4 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int considered = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            List<Cloudlet> assigned = readSpace.getVmCloudletList(vm);
            if (!assigned.isEmpty()) {
                continue;
            }

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ",
                    "VM " + vmId + " underloaded and idle (no workload), requesting destruction to reclaim capacity.");
            return new int[]{vmId};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v4] ",
                "No idle underloaded VM eligible for reaping.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-underload-idle";
    }

    @Override
    public String outputSemantic() {
        return "vm-destruction-reap";
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

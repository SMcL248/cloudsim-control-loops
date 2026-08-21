package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 1: Reactive greedy vertical scale-up.
 * Strategy: scan VM-level diagnosis in order and grant one extra PE to the
 * first OVERLOADED VM found. No feasibility lookahead, no host awareness -
 * a minimal-latency, first-responder policy that trades optimality for speed.
 */
public class planner_v1 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int considered = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ",
                    "VM " + vmId + " flagged OVERLOADED, requesting immediate PE allocation.");
            return new int[]{vmId};
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v1] ",
                "No overloaded VM found, no PE allocation requested.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-overload-pe-demand";
    }

    @Override
    public String outputSemantic() {
        return "pe-allocation";
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

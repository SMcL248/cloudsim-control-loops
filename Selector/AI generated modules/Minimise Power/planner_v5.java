package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

/**
 * Variant 5 - Zero-Workload VM Garbage Collection.
 *
 * Strategy: deliberately ignores utilisation heuristics in favour of a hard
 * factual check - a VM with literally no assigned cloudlets, that is not
 * mid-migration and not still being instantiated, is pure dead weight. It
 * consumes host power and provisioned resources while doing zero work and
 * risking zero further work loss. This is the most drastic action in the
 * suite (full VM destruction) but the safest possible trigger for it.
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3004;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        GuestEntity deadWeightVm = null;

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) {
                continue;
            }
            List<Cloudlet> owned = readSpace.getVmCloudletList(vm);
            if (!owned.isEmpty()) {
                continue;
            }
            deadWeightVm = vm;
            break;
        }

        int[] noOp = new int[]{-1};
        if (deadWeightVm == null) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v5] no zero-workload vm found, emitting no-op");
            return noOp;
        }

        int vmId = readSpace.getId(deadWeightVm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v5] destroying zero-workload vm ", vmId);
        return new int[]{vmId};
    }

    @Override
    public String inputSemantic() {
        return "vm-workload-presence-loadstate";
    }

    @Override
    public String outputSemantic() {
        return "requestVmDestruction";
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

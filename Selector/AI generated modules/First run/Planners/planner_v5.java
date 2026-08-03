package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v5 - Idle VM destruction planner (power-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). Looks for an UNDERLOADED VM that is not
 *   migrating, not mid-instantiation, and has no cloudlets left to process,
 *   and requests it be destroyed to stop it drawing power.
 *
 * Input semantic  : vm-loadstate-idle (GUID 2300)
 * Output semantic : vm-destruction    (GUID 3008, requestVmDestruction)
 */
public class planner_v5 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v5] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1};
        }

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;

            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            List<Cloudlet> remaining = readSpace.getVmCloudletList(vm);
            if (!remaining.isEmpty()) continue;

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [planner_v5] Plan destroy idle, drained VM ", vmId);
            return new int[]{vmId};
        }

        Log.printlnConcat(now, ": [planner_v5] No idle, drained VM to destroy. No-op.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-idle";
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
        return 3008;
    }
}

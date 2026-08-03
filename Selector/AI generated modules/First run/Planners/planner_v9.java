package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v9 - Slack-core deallocation planner (power-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). Finds the first UNDERLOADED VM that
 *   holds more than one PE and requests one be deallocated, freeing
 *   capacity that would otherwise sit idle and draw power.
 *
 * Input semantic  : vm-loadstate-pe-slack (GUID 2300)
 * Output semantic : pe-deallocation       (GUID 3013, requestPeDeallocation)
 */
public class planner_v9 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v9] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1};
        }

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;

            GuestEntity vm = vms.get(i);
            if (readSpace.getVmNumberOfPes(vm) <= 1) continue;

            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [planner_v9] Plan deallocate one spare PE from underloaded VM ", vmId);
            return new int[]{vmId};
        }

        Log.printlnConcat(now, ": [planner_v9] No underloaded VM with a spare PE. No-op.");
        return new int[]{-1};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-pe-slack";
    }

    @Override
    public String outputSemantic() {
        return "pe-deallocation";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3013;
    }
}

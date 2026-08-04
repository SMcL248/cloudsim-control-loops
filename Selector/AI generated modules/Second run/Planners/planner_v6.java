package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level planner. diagnosis[i] is the load state of readSpace.getVmList().get(i).
// Goal: minimise total raw energy consumed.
// Strategy: an UNDERLOADED VM is provisioned with more bandwidth than it is
// using. Step its BW allocation down one tier (matched against
// getBwTiers()) to trim the resource footprint that drives host power draw,
// without touching MIPS/RAM so cloudlet execution is unaffected.
public class planner_v6 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v6";
    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3007;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/VM size mismatch, no-op");
            return new int[]{-1, -1};
        }

        int[] bwTiers = readSpace.getBwTiers();
        if (bwTiers == null || bwTiers.length == 0) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no bw tiers available, no-op");
            return new int[]{-1, -1};
        }

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            int currentIndex = closestTierIndex(bwTiers, readSpace.getVmBw(vm));
            if (currentIndex <= 0) continue; // already at lowest tier

            int targetIndex = currentIndex - 1;
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan scale down VM ", vmId,
                    " BW from tier ", currentIndex, " to tier ", targetIndex);
            return new int[]{vmId, targetIndex};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no underloaded VM eligible for BW scale-down, no-op");
        return new int[]{-1, -1};
    }

    private int closestTierIndex(int[] tiers, double value) {
        int bestIndex = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < tiers.length; i++) {
            double diff = Math.abs(tiers[i] - value);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    @Override
    public String inputSemantic() {
        return "vm-bw-idle-underload";
    }

    @Override
    public String outputSemantic() {
        return "requestbwscaling";
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

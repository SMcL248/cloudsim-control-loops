package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level planner. diagnosis[i] is the load state of readSpace.getVmList().get(i).
// Goal: maximise throughput / minimise makespan.
// Strategy: for the first OVERLOADED VM that is not already at the top MIPS
// tier, request a bump to the next tier (matched against getMipsTiers()),
// so it can process its backlog faster instead of falling further behind.
public class planner_v4 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v4";
    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3005;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/VM size mismatch, no-op");
            return new int[]{-1, -1};
        }

        int[] mipsTiers = readSpace.getMipsTiers();
        if (mipsTiers == null || mipsTiers.length == 0) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no mips tiers available, no-op");
            return new int[]{-1, -1};
        }

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            int currentIndex = closestTierIndex(mipsTiers, readSpace.getVmMips(vm));
            if (currentIndex >= mipsTiers.length - 1) continue; // already at top tier

            int targetIndex = currentIndex + 1;
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan scale VM ", vmId,
                    " MIPS from tier ", currentIndex, " to tier ", targetIndex);
            return new int[]{vmId, targetIndex};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no overloaded VM eligible for MIPS scaling, no-op");
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
        return "vm-mips-congestion-overload";
    }

    @Override
    public String outputSemantic() {
        return "requestmipsscaling";
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

package org.cloudbus.cloudsim.examples;// always include

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// VM-level planner. diagnosis[i] is the load state of readSpace.getVmList().get(i).
// Goal: maximise service availability - preserve VM capacity for incoming
// work.
// Strategy: an OVERLOADED VM is close to saturating its allocated RAM, and
// is at risk of being unable to accept incoming cloudlets. Step its RAM
// allocation up one tier (matched against getRamTiers()) ahead of that
// point, giving it headroom before new work arrives rather than reacting
// after it is already starved.
public class planner_v9 implements Planner<LoadState[], int[]> {

    private static final String MODULE_NAME = "planner_v9";
    private static final int INPUT_GUID = 2300;
    private static final int OUTPUT_GUID = 3006;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] diagnosis/VM size mismatch, no-op");
            return new int[]{-1, -1};
        }

        int[] ramTiers = readSpace.getRamTiers();
        if (ramTiers == null || ramTiers.length == 0) {
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] no ram tiers available, no-op");
            return new int[]{-1, -1};
        }

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;
            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            int currentIndex = closestTierIndex(ramTiers, readSpace.getVmRam(vm));
            if (currentIndex >= ramTiers.length - 1) continue; // already at top tier

            int targetIndex = currentIndex + 1;
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [" + MODULE_NAME + "] plan scale up VM ", vmId,
                    " RAM from tier ", currentIndex, " to tier ", targetIndex,
                    " to preserve capacity for incoming work");
            return new int[]{vmId, targetIndex};
        }

        Log.printlnConcat(now, ": [" + MODULE_NAME + "] no overloaded VM eligible for RAM scale-up, no-op");
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
        return "requestramscaling";
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

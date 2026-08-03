package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v7 - Memory vertical scale-up planner (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). Finds the first OVERLOADED VM that still
 *   has RAM headroom (its next RAM tier exceeds its current RAM) and
 *   requests scaling it up to that next tier, relieving memory pressure
 *   that would otherwise throttle throughput.
 *
 * Input semantic  : vm-loadstate-mem-bound (GUID 2300)
 * Output semantic : ram-scaling            (GUID 3010, requestRamScaling)
 */
public class planner_v7 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v7] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1, -1};
        }

        int[] ramTiers = readSpace.getRamTiers();

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            double current = readSpace.getVmRam(vm);
            double next = readSpace.getNextRamTier(vm);
            if (next <= current) continue; // already at ceiling

            int tierIndex = closestTierIndex(ramTiers, next);
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [planner_v7] Plan scale VM ", vmId,
                    " RAM ", current, " -> tier ", tierIndex, " (~", next, ")");
            return new int[]{vmId, tierIndex};
        }

        Log.printlnConcat(now, ": [planner_v7] No overloaded VM with RAM headroom. No-op.");
        return new int[]{-1, -1};
    }

    private int closestTierIndex(int[] tiers, double target) {
        int bestIdx = 0;
        double bestDiff = Double.MAX_VALUE;
        for (int i = 0; i < tiers.length; i++) {
            double diff = Math.abs(tiers[i] - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-mem-bound";
    }

    @Override
    public String outputSemantic() {
        return "ram-scaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3010;
    }
}

package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

/**
 * Planner v6 - CPU vertical scale-up planner (throughput-oriented).
 *
 * Strategy:
 *   Diagnosis is a per-VM LoadState[] (index i corresponds to
 *   readSpace.getVmList().get(i)). Finds the first OVERLOADED VM that still
 *   has MIPS headroom (its next MIPS tier exceeds its current MIPS) and
 *   requests scaling it up to that next tier, raising its throughput
 *   ceiling.
 *
 * Input semantic  : vm-loadstate-cpu-bound (GUID 2300)
 * Output semantic : mips-scaling           (GUID 3009, requestMipsScaling)
 */
public class planner_v6 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        double now = readSpace.getNow();
        List<GuestEntity> vms = readSpace.getVmList();

        if (diagnosis == null || diagnosis.length != vms.size()) {
            Log.printlnConcat(now, ": [planner_v6] Diagnosis/VM size mismatch. No-op.");
            return new int[]{-1, -1};
        }

        int[] mipsTiers = readSpace.getMipsTiers();

        for (int i = 0; i < vms.size(); i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) continue;

            GuestEntity vm = vms.get(i);
            if (readSpace.isVmMigrating(vm) || readSpace.isVmBeingInstantiated(vm)) continue;

            double current = readSpace.getVmMips(vm);
            double next = readSpace.getNextMipsTier(vm);
            if (next <= current) continue; // already at ceiling

            int tierIndex = closestTierIndex(mipsTiers, next);
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(now, ": [planner_v6] Plan scale VM ", vmId,
                    " MIPS ", current, " -> tier ", tierIndex, " (~", next, ")");
            return new int[]{vmId, tierIndex};
        }

        Log.printlnConcat(now, ": [planner_v6] No overloaded VM with MIPS headroom. No-op.");
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
        return "vm-loadstate-cpu-bound";
    }

    @Override
    public String outputSemantic() {
        return "mips-scaling";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3009;
    }
}

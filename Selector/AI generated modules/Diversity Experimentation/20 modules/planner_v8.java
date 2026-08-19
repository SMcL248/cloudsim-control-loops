package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v8 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int limit = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.OVERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            double nextTierValue = readSpace.getNextRamTier(vm);
            if (nextTierValue < 0) {
                continue;
            }
            int tierIndex = tierIndexOf(readSpace.getRamTiers(), nextTierValue);
            if (tierIndex < 0) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            Log.printlnConcat(readSpace.getNow(), ": [planner_v8] scaling VM ", vmId, " RAM up to tier ", tierIndex);
            return new int[] { vmId, tierIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v8] no overloaded VM eligible for RAM scale-up");
        return new int[0];
    }

    private int tierIndexOf(int[] tiers, double value) {
        int rounded = (int) Math.round(value);
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == rounded) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-memory-pressure-proxy";
    }

    @Override
    public String outputSemantic() {
        return "ram-scale-up";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3006;
    }
}

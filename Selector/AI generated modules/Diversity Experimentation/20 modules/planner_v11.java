package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.List;

public class planner_v11 implements Planner<LoadState[], int[]> {

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int[] tiers = readSpace.getBwTiers();
        int limit = Math.min(diagnosis.length, vms.size());

        for (int i = 0; i < limit; i++) {
            if (diagnosis[i] != LoadState.UNDERLOADED) {
                continue;
            }
            GuestEntity vm = vms.get(i);
            int currentIndex = tierIndexOf(tiers, readSpace.getVmBw(vm));
            if (currentIndex <= 0) {
                continue;
            }
            int vmId = readSpace.getId(vm);
            int targetIndex = currentIndex - 1;
            Log.printlnConcat(readSpace.getNow(), ": [planner_v11] scaling VM ", vmId, " bandwidth down to tier ", targetIndex);
            return new int[] { vmId, targetIndex };
        }

        Log.printlnConcat(readSpace.getNow(), ": [planner_v11] no underloaded VM eligible for bandwidth scale-down");
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
        return "vm-loadstate-bandwidth-slack";
    }

    @Override
    public String outputSemantic() {
        return "bw-scale-down";
    }

    @Override
    public int inputGuid() {
        return 2300;
    }

    @Override
    public int outputGuid() {
        return 3007;
    }
}

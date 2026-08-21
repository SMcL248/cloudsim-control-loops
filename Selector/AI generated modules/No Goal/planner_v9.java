package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * Variant 9: Stateful fairness round-robin bandwidth scaling.
 * Strategy: read VM-level diagnosis. Rather than always favouring the
 * lowest-index OVERLOADED VM (which would starve later VMs of scaling
 * attention cycle after cycle), this planner remembers the last VM index
 * it served across invocations and advances to the next OVERLOADED VM in
 * rotation, wrapping around when it reaches the end of the list.
 */
public class planner_v9 implements Planner<LoadState[], int[]> {

    private int lastServedIndex = -1;

    @Override
    public int[] plan(LoadState[] diagnosis, ReadSpace readSpace) {
        List<GuestEntity> vms = readSpace.getVmList();
        int considered = Math.min(diagnosis.length, vms.size());

        List<Integer> overloadedIndices = new ArrayList<Integer>();
        for (int i = 0; i < considered; i++) {
            if (diagnosis[i] == LoadState.OVERLOADED) {
                overloadedIndices.add(i);
            }
        }

        if (overloadedIndices.isEmpty()) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ",
                    "No overloaded VMs, fairness rotation idle.");
            return new int[]{-1, -1};
        }

        int chosenIndex = overloadedIndices.get(0);
        for (int idx : overloadedIndices) {
            if (idx > lastServedIndex) {
                chosenIndex = idx;
                break;
            }
        }

        GuestEntity vm = vms.get(chosenIndex);
        double nextBw = readSpace.getNextBwTier(vm);
        lastServedIndex = chosenIndex;

        if (nextBw < 0) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ",
                    "VM " + readSpace.getId(vm) + " selected by rotation but already at max BW tier, skipping turn.");
            return new int[]{-1, -1};
        }

        int[] bwTiers = readSpace.getBwTiers();
        int tierIndex = -1;
        for (int t = 0; t < bwTiers.length; t++) {
            if (bwTiers[t] == (int) nextBw) {
                tierIndex = t;
                break;
            }
        }

        if (tierIndex == -1) {
            Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ",
                    "VM " + readSpace.getId(vm) + " next BW tier unresolvable, skipping turn.");
            return new int[]{-1, -1};
        }

        int vmId = readSpace.getId(vm);
        Log.printlnConcat(readSpace.getNow(), ": [planner_v9] ",
                "Fairness rotation selects VM " + vmId + " for BW scale to tier " + tierIndex);
        return new int[]{vmId, tierIndex};
    }

    @Override
    public String inputSemantic() {
        return "vm-loadstate-overload-bwpressure";
    }

    @Override
    public String outputSemantic() {
        return "bw-scaling-fair";
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

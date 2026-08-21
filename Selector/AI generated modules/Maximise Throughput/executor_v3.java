package org.cloudbus.cloudsim.examples;// always include

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// GUID 3003 -- requestVmCreation
// Strategy: capacity-expansion executor. Validates only structural sanity of
// the requested tiers (non-negative), then always attempts creation and lets
// the platform be the authority on whether the tier/datacenter combination is
// valid, logging the outcome either way -- new capacity is the direct lever
// for raising achievable throughput.
public class executor_v3 implements Executor<int[]> {

    private static final int GUID = 3003;

    @Override
    public boolean execute(int[] action, ActionSpace actionSpace) {
        if (action == null || action.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Malformed payload for requestVmCreation, expected 3 ints, aborting.");
            return false;
        }
        if (isSentinel(action)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] No prescribed action this cycle.");
            return false;
        }

        int tierIndex = action[0];
        int sizeTierIndex = action[1];
        int datacenterId = action[2];

        if (tierIndex < 0 || sizeTierIndex < 0 || datacenterId < 0) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Negative tier/datacenter index, aborting VM creation request.");
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);
        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] VM creation request rejected by platform for tier " + tierIndex + ", size " + sizeTierIndex + ", datacenter " + datacenterId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] Created new VM " + actionSpace.getId(created) + " (tier " + tierIndex + ", size " + sizeTierIndex + ") in datacenter " + datacenterId);
        }
        return true;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Create a new VM at a given MIPS/size tier in a datacenter to expand available processing capacity";
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}

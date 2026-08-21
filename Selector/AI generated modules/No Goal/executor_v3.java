package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Strategy: bounds-checked provisioning.
// Validates the requested tier index against the platform's known
// MIPS/RAM/BW tier tables (and sanity-checks size tier and datacenter id)
// before spending a creation request. A null result from the platform is
// logged but still counts as "attempted" -- the request was well-formed
// and made, the platform simply declined it.
public class executor_v3 implements Executor<int[]> {

    private static final int INPUT_GUID = 3003;
    private static final String INPUT_SEMANTIC = "create a new VM at a given tier and size in a datacenter";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        if (!hasShape(actions, 3) || isSentinel(actions)) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] no-op or malformed payload received, skipping requestVmCreation");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        int[] mipsTiers = actionSpace.getMipsTiers();
        int[] ramTiers = actionSpace.getRamTiers();
        int[] bwTiers = actionSpace.getBwTiers();

        boolean tierInBounds = tierIndex >= 0
                && tierIndex < mipsTiers.length
                && tierIndex < ramTiers.length
                && tierIndex < bwTiers.length;

        if (!tierInBounds || sizeTierIndex < 0 || datacenterId < 0) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] tier index ", tierIndex, ", size tier ", sizeTierIndex, " or datacenter ", datacenterId, " out of bounds, skipping creation");
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (created == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] VM creation request at tier ", tierIndex, " in datacenter ", datacenterId, " was declined by the platform");
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v3] requested creation of VM ", actionSpace.getId(created), " at tier ", tierIndex, " in datacenter ", datacenterId);
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return INPUT_SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return INPUT_GUID;
    }

    private boolean hasShape(int[] a, int len) {
        return a != null && a.length == len;
    }

    private boolean isSentinel(int[] a) {
        for (int v : a) {
            if (v != -1) {
                return false;
            }
        }
        return true;
    }
}

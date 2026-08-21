package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

// Executor for ActionSpace.requestVmCreation - GUID suffix 03.
public class executor_v3 implements Executor<int[]> {

    private static final int GUID = 3003;
    private static final String SEMANTIC =
        "requestVmCreation: request a new VM at a given MIPS/RAM/BW/core tier and storage size tier in a target datacenter. Payload {tierIndex, sizeTierIndex, datacenterId}.";

    @Override
    public boolean execute(int[] actions, ActionSpace actionSpace) {
        double now = actionSpace.getNow();

        if (actions == null || actions.length != 3) {
            Log.printlnConcat(now, ": [executor_v3] rejected - payload must have exactly 3 entries {tierIndex, sizeTierIndex, datacenterId}.");
            return false;
        }

        if (actions[0] == -1 && actions[1] == -1 && actions[2] == -1) {
            Log.printlnConcat(now, ": [executor_v3] no-op sentinel received, no VM requested.");
            return false;
        }

        int tierIndex = actions[0];
        int sizeTierIndex = actions[1];
        int datacenterId = actions[2];

        int tierCount = actionSpace.getMipsTiers().length;
        if (tierIndex < 0 || tierIndex >= tierCount) {
            Log.printlnConcat(now, ": [executor_v3] rejected - tierIndex ", tierIndex, " is outside the known tier range [0,", tierCount, ").");
            return false;
        }

        if (sizeTierIndex < 0 || datacenterId < 0) {
            Log.printlnConcat(now, ": [executor_v3] rejected - sizeTierIndex or datacenterId is negative (sizeTierIndex=", sizeTierIndex,
                ", datacenterId=", datacenterId, ").");
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        if (created == null) {
            Log.printlnConcat(now, ": [executor_v3] attempted VM creation at tierIndex ", tierIndex, " but the request was refused (null returned).");
        } else {
            Log.printlnConcat(now, ": [executor_v3] created VM ", actionSpace.getId(created), " at tierIndex ", tierIndex, " in datacenter ", datacenterId, ".");
        }
        return true;
    }

    @Override
    public String inputSemantic() {
        return SEMANTIC;
    }

    @Override
    public int inputGuid() {
        return GUID;
    }
}

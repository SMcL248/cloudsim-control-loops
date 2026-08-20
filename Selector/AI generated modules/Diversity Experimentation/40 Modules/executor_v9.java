package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v9 implements Executor<int[]> {

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 3) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ",
                    "malformed payload, expected {tierIndex, sizeTierIndex, datacenterId}");
            return false;
        }

        int tierIndex = payload[0];
        int sizeTierIndex = payload[1];
        int datacenterId = payload[2];

        int mipsTierCount = actionSpace.getMipsTiers().length;
        int ramTierCount = actionSpace.getRamTiers().length;

        if (tierIndex < 0 || tierIndex >= mipsTierCount) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ",
                    "rejecting creation, tierIndex " + tierIndex + " out of range [0," + mipsTierCount + ")");
            return false;
        }

        if (sizeTierIndex < 0 || sizeTierIndex >= ramTierCount) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ",
                    "rejecting creation, sizeTierIndex " + sizeTierIndex + " out of range [0," + ramTierCount + ")");
            return false;
        }

        if (datacenterId < 0) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ",
                    "rejecting creation, negative datacenterId " + datacenterId);
            return false;
        }

        GuestEntity created = actionSpace.requestVmCreation(tierIndex, sizeTierIndex, datacenterId);

        Log.printlnConcat(actionSpace.getNow(), ": [executor_v9] ",
                "bounds check passed, issued requestVmCreation, result=" + (created == null ? "null" : "vm created"));
        return true;
    }

    @Override
    public String inputSemantic() {
        return "Create a new VM";
    }

    @Override
    public int inputGuid() {
        return 3003;
    }
}

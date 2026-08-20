package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.GuestEntity;

public class executor_v18 implements Executor<int[]> {

    private static final int MAX_ATTEMPTS = 3;

    private int successfulActionCount = 0;

    @Override
    public boolean execute(int[] payload, ActionSpace actionSpace) {
        if (payload == null || payload.length != 2) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                    "malformed payload, expected {vmId, tierIndex}");
            return false;
        }

        int vmId = payload[0];
        int tierIndex = payload[1];

        GuestEntity vm = actionSpace.getVmById(vmId);
        if (vm == null) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                    "unresolved VM reference for id " + vmId);
            return false;
        }

        int[] tiers = actionSpace.getMipsTiers();
        if (tierIndex < 0 || tierIndex >= tiers.length) {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                    "tierIndex " + tierIndex + " out of range, scaling not attempted");
            return false;
        }

        boolean succeeded = false;
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS && !succeeded) {
            attempt++;
            succeeded = actionSpace.requestMipsScaling(vm, tiers[tierIndex]);
            if (!succeeded) {
                Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                        "requestMipsScaling attempt " + attempt + " of " + MAX_ATTEMPTS + " failed for VM " + vmId);
            }
        }

        if (succeeded) {
            successfulActionCount++;
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                    "requestMipsScaling succeeded on attempt " + attempt + " for VM " + vmId);
        } else {
            Log.printlnConcat(actionSpace.getNow(), ": [executor_v18] ",
                    "requestMipsScaling exhausted " + MAX_ATTEMPTS + " attempts for VM " + vmId);
        }

        return true;
    }

    @Override
    public String inputSemantic() {
        return "Scale VM MIPS to a tier";
    }

    @Override
    public int inputGuid() {
        return 3005;
    }

    @Override
    public int getSuccessfulActionCount() {
        return successfulActionCount;
    }
}
